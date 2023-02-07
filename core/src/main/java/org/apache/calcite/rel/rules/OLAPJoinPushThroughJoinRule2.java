/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.rel.rules;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelRule;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexPermuteInputsShuttle;
import org.apache.calcite.rex.RexUtil;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.tools.RelBuilderFactory;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.calcite.util.Permutation;
import org.apache.calcite.util.mapping.AbstractTargetMapping;
import org.apache.calcite.util.mapping.Mapping;
import org.apache.calcite.util.mapping.Mappings;

import org.apache.kylin.guava30.shaded.common.base.Preconditions;

import org.immutables.value.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * modified form org.apache.calcite.rel.rules.JoinPushThroughJoinRule.
 * The goal is to move joins with sub-queries after joins with tables,
 * so that pre-defined join with tables can be matched
 * differ from OLAPJoinPushThroughJoinRule in the pattern to
 * match. OLAPJoinPushThroughJoinRule
 * will generate a result pattern which cannot recursively
 * match OLAPJoinPushThroughJoinRule's pattern.
 * So OLAPJoinPushThroughJoinRule2 is introduced to allow recursive matching
 */
@Value.Enclosing
public class OLAPJoinPushThroughJoinRule2 extends RelRule<OLAPJoinPushThroughJoinRule2.Config> {
  /**
   * Instance of the rule that works on logical joins only, and pushes to the
   * right.
   */
  public static final RelOptRule INSTANCE = new OLAPJoinPushThroughJoinRule2(
          "OLAPJoinPushThroughJoinRule2", LogicalJoin.class, RelFactories.LOGICAL_BUILDER);

  protected OLAPJoinPushThroughJoinRule2(Config config) {
    super(config);
  }

  public OLAPJoinPushThroughJoinRule2(String description, Class<? extends Join> joinClass,
           RelBuilderFactory relBuilderFactory) {
    this(Config.DEFAULT.withDescription(description)
        .withRelBuilderFactory(relBuilderFactory)
        .as(Config.class)
        .withOperandFor(joinClass));
  }

  @Override public void onMatch(RelOptRuleCall call) {
    onMatchRight(call);
  }

  private void onMatchRight(RelOptRuleCall call) {
    final Join topJoin = call.rel(0);
    final Project projectOnBottomJoin = call.rel(1);
    final Join bottomJoin = call.rel(2);
    final RelNode relC = call.rel(5);
    final RelNode relA = bottomJoin.getLeft();
    final RelNode relB = bottomJoin.getRight();
    final RelOptCluster cluster = topJoin.getCluster();
    final Permutation projectPermu = projectOnBottomJoin.getPermutation();
    final Permutation inverseProjectPermu = projectPermu.inverse();
    //        Preconditions.checkState(relA == call.rel(3));
    //        Preconditions.checkState(relB == call.rel(4));
    //    Preconditions.checkNotNull(projectPermu);

    //            topJoin
    //           /        \
    //        project      C
    //        /
    //   bottomJoin
    //    /    \
    //   A      B

    final int aCount = relA.getRowType().getFieldCount();
    final int bCount = relB.getRowType().getFieldCount();
    final int cCount = relC.getRowType().getFieldCount();
    final ImmutableBitSet bBitSetBelowProject = ImmutableBitSet.range(aCount, aCount + bCount);
    final ImmutableBitSet bBitSetAboveProject = Mappings.apply(inverseProjectPermu,
            bBitSetBelowProject);

    final Mapping extendedProjectPerm = createAbstractTargetMapping(
            Mappings.append(projectPermu, Mappings.createIdentity(cCount)));

    // becomes
    //
    //            project
    //             /
    //        newTopJoin
    //        /        \
    //   newBottomJoin  B
    //    /    \
    //   A      C

    // If either join is not inner, we cannot proceed.
    // (Is this too strict?)
    //        if (topJoin.getJoinType() != JoinRelType.INNER ||
    // bottomJoin.getJoinType() != JoinRelType.INNER) {
    //            return;
    //        }

    // right join cannot be pushed through inner
    if (topJoin.getJoinType() == JoinRelType.RIGHT
            && bottomJoin.getJoinType() == JoinRelType.INNER) {
      return;
    }

    // Split the condition of topJoin into a conjunction. Each of the
    // parts that does not use columns from B can be pushed down.
    final List<RexNode> intersecting = new ArrayList<>();
    final List<RexNode> nonIntersecting = new ArrayList<>();
    split(topJoin.getCondition(), bBitSetAboveProject, intersecting, nonIntersecting);

    // If there's nothing to push down, it's not worth proceeding.
    if (nonIntersecting.isEmpty()) {
      return;
    }

    // Split the condition of bottomJoin into a conjunction. Each of the
    // parts that use columns from B will need to be pulled up.
    final List<RexNode> bottomIntersecting = new ArrayList<>();
    final List<RexNode> bottomNonIntersecting = new ArrayList<>();
    split(bottomJoin.getCondition(), bBitSetBelowProject,
            bottomIntersecting, bottomNonIntersecting);
    Preconditions.checkState(bottomNonIntersecting.isEmpty());

    // target: | A       | C      |
    // source: | A       | B | C      |
    final Mappings.TargetMapping tempMapping = Mappings.createShiftMapping(
            aCount + bCount + cCount, 0, 0, aCount,
            aCount + cCount, aCount, bCount, aCount, aCount + bCount, cCount);
    final Mappings.TargetMapping thruProjectMapping = Mappings.multiply(
            extendedProjectPerm, createAbstractTargetMapping(tempMapping));
    final List<RexNode> newBottomList = new ArrayList<>();
    new RexPermuteInputsShuttle(thruProjectMapping, relA, relC)
            .visitList(nonIntersecting, newBottomList);
    final RexBuilder rexBuilder = cluster.getRexBuilder();
    RexNode newBottomCondition = RexUtil.composeConjunction(rexBuilder, newBottomList, false);
    final Join newBottomJoin = bottomJoin.copy(bottomJoin.getTraitSet(),
            newBottomCondition, relA, relC, topJoin.getJoinType(), bottomJoin.isSemiJoinDone());

    // target: | A       | C      | B |
    // source: | A       | B | C      |
    final Mappings.TargetMapping nonThruProjectMapping = Mappings.createShiftMapping(
            aCount + bCount + cCount, 0, 0, aCount, aCount + cCount,
            aCount, bCount, aCount, aCount + bCount, cCount);
    final List<RexNode> newTopList = new ArrayList<>();
    new RexPermuteInputsShuttle(thruProjectMapping, newBottomJoin, relB)
            .visitList(intersecting, newTopList);
    new RexPermuteInputsShuttle(nonThruProjectMapping, newBottomJoin, relB)
            .visitList(bottomIntersecting, newTopList);
    RexNode newTopCondition = RexUtil.composeConjunction(rexBuilder, newTopList, false);
    @SuppressWarnings("SuspiciousNameCombination")
    final Join newTopJoin = topJoin.copy(topJoin.getTraitSet(), newTopCondition,
            newBottomJoin, relB, bottomJoin.getJoinType(), topJoin.isSemiJoinDone());

    assert !Mappings.isIdentity(thruProjectMapping);
    final RelBuilder relBuilder = call.builder();
    relBuilder.push(newTopJoin);
    relBuilder.project(relBuilder.fields(thruProjectMapping));
    call.transformTo(relBuilder.build());
  }

  private AbstractTargetMapping createAbstractTargetMapping(
          final Mappings.TargetMapping targetMapping) {
    return new AbstractTargetMapping(targetMapping.getSourceCount(),
            targetMapping.getTargetCount()) {
      @Override public int getTargetOpt(int source) {
        return targetMapping.getTargetOpt(source);
      }
    };
  }

  /**
   * Splits a condition into conjunctions that do or do not intersect with
   * a given bit set.
   */
  static void split(RexNode condition, ImmutableBitSet bitSet, List<RexNode> intersecting,
                    List<RexNode> nonIntersecting) {
    for (RexNode node : RelOptUtil.conjunctions(condition)) {
      ImmutableBitSet inputBitSet = RelOptUtil.InputFinder.bits(node);
      if (bitSet.intersects(inputBitSet)) {
        intersecting.add(node);
      } else {
        nonIntersecting.add(node);
      }
    }
  }

  /** Rule configuration. */
  @Value.Immutable
  public interface Config extends RelRule.Config {
    Config DEFAULT = ImmutableOLAPJoinPushThroughJoinRule2.Config.of()
        .withDescription("OLAPJoinPushThroughJoinRule2")
        .withOperandFor(LogicalJoin.class);

    @Override default OLAPJoinPushThroughJoinRule2 toRule() {
      return new OLAPJoinPushThroughJoinRule2(this);
    }

    /** Defines an operand tree for the given classes. */
    default Config withOperandFor(Class<? extends Join> joinClass) {
      return withOperandSupplier(b0 ->
          b0.operand(joinClass).inputs(b1 ->
                  b1.operand(Project.class).predicate(input -> input.getPermutation() != null)
                      .inputs(
                          b2 -> b2.operand(joinClass).inputs(
                              b11 ->
                                  b11.operand(RelNode.class).anyInputs(),
                              b12 ->
                                  b12.operand(RelNode.class).predicate(
                                      relNode -> !(relNode instanceof TableScan)
                                  ).anyInputs())),
              b2 -> b2.operand(TableScan.class).anyInputs()))
          .as(Config.class);
    }
  }
}
