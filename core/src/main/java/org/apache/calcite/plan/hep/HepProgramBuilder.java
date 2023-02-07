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
package org.apache.calcite.plan.hep;

import org.apache.calcite.plan.CommonRelSubExprRule;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptRule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.kylin.guava30.shaded.common.base.Preconditions.checkArgument;

/**
 * HepProgramBuilder creates instances of {@link HepProgram}.
 */
public class HepProgramBuilder {
  //~ Instance fields --------------------------------------------------------

  private final List<HepInstruction> instructions = new ArrayList<>();

  /** If a group is under construction, ordinal of the first instruction in the
   * group; otherwise -1. */
  private int group = -1;

  //~ Constructors -----------------------------------------------------------

  /**
   * Creates a new HepProgramBuilder with an initially empty program. The
   * program under construction has an initial match order of
   * {@link HepMatchOrder#DEPTH_FIRST}, and an initial match limit of
   * {@link HepProgram#MATCH_UNTIL_FIXPOINT}.
   */
  public HepProgramBuilder() {
  }

  //~ Methods ----------------------------------------------------------------

  private void clear() {
    instructions.clear();
    group = -1;
  }

  /**
   * Adds an instruction to attempt to match any rules of a given class. The
   * order in which the rules within a class will be attempted is arbitrary,
   * so if more control is needed, use addRuleInstance instead.
   *
   * <p>Note that when this method is used, it is also necessary to add the
   * actual rule objects of interest to the planner via
   * {@link RelOptPlanner#addRule}. If the planner does not have any
   * rules of the given class, this instruction is a nop.
   *
   * <p>TODO: support classification via rule annotations.
   *
   * @param ruleClass class of rules to fire, e.g. ConverterRule.class
   */
  public <R extends RelOptRule> HepProgramBuilder addRuleClass(
      Class<R> ruleClass) {
    return addInstruction(new HepInstruction.RuleClass(ruleClass));
  }

  /**
   * Adds an instruction to attempt to match any rules in a given collection.
   * The order in which the rules within a collection will be attempted is
   * arbitrary, so if more control is needed, use addRuleInstance instead. The
   * collection can be "live" in the sense that not all rule instances need to
   * have been added to it at the time this method is called. The collection
   * contents are reevaluated for each execution of the program.
   *
   * <p>Note that when this method is used, it is NOT necessary to add the
   * rules to the planner via {@link RelOptPlanner#addRule}; the instances
   * supplied here will be used. However, adding the rules to the planner
   * redundantly is good form since other planners may require it.
   *
   * @param rules collection of rules to fire
   */
  public HepProgramBuilder addRuleCollection(Collection<RelOptRule> rules) {
    return addInstruction(new HepInstruction.RuleCollection(rules));
  }

  /**
   * Adds an instruction to attempt to match a specific rule object.
   *
   * <p>Note that when this method is used, it is NOT necessary to add the
   * rule to the planner via {@link RelOptPlanner#addRule}; the instance
   * supplied here will be used. However, adding the rule to the planner
   * redundantly is good form since other planners may require it.
   *
   * @param rule rule to fire
   */
  public HepProgramBuilder addRuleInstance(RelOptRule rule) {
    return addInstruction(new HepInstruction.RuleInstance(rule));
  }

  /**
   * Adds an instruction to attempt to match a specific rule identified by its
   * unique description.
   *
   * <p>Note that when this method is used, it is necessary to also add the
   * rule object of interest to the planner via {@link RelOptPlanner#addRule}.
   * This allows for some decoupling between optimizers and plugins: the
   * optimizer only knows about rule descriptions, while the plugins supply
   * the actual instances. If the planner does not have a rule matching the
   * description, this instruction is a nop.
   *
   * @param ruleDescription description of rule to fire
   */
  public HepProgramBuilder addRuleByDescription(String ruleDescription) {
    return addInstruction(
        new HepInstruction.RuleLookup(ruleDescription));
  }

  /**
   * Adds an instruction to begin a group of rules. All subsequent rules added
   * (until the next endRuleGroup) will be collected into the group rather
   * than firing individually. After addGroupBegin has been called, only
   * addRuleXXX methods may be called until the next addGroupEnd.
   */
  public HepProgramBuilder addGroupBegin() {
    checkArgument(group < 0);
    group = instructions.size();
    return addInstruction(new HepInstruction.Placeholder());
  }

  /**
   * Adds an instruction to end a group of rules, firing the group
   * collectively. The order in which the rules within a group will be
   * attempted is arbitrary. Match order and limit applies to the group as a
   * whole.
   */
  public HepProgramBuilder addGroupEnd() {
    checkArgument(group >= 0);
    final HepInstruction.EndGroup endGroup = new HepInstruction.EndGroup();
    instructions.set(group, new HepInstruction.BeginGroup(endGroup));
    group = -1;
    return addInstruction(endGroup);
  }

  /**
   * Adds an instruction to attempt to match instances of
   * {@link org.apache.calcite.rel.convert.ConverterRule},
   * but only where a conversion is actually required.
   *
   * @param guaranteed if true, use only guaranteed converters; if false, use
   *                   only non-guaranteed converters
   */
  public HepProgramBuilder addConverters(boolean guaranteed) {
    checkArgument(group < 0);
    return addInstruction(new HepInstruction.ConverterRules(guaranteed));
  }

  /**
   * Adds an instruction to attempt to match instances of
   * {@link CommonRelSubExprRule}, but only in cases where vertices have more
   * than one parent.
   */
  public HepProgramBuilder addCommonRelSubExprInstruction() {
    checkArgument(group < 0);
    return addInstruction(new HepInstruction.CommonRelSubExprRules());
  }

  /**
   * Adds an instruction to change the order of pattern matching for
   * subsequent instructions. The new order will take effect for the rest of
   * the program (not counting subprograms) or until another match order
   * instruction is encountered.
   *
   * @param order new match direction to set
   */
  public HepProgramBuilder addMatchOrder(HepMatchOrder order) {
    checkArgument(group < 0);
    return addInstruction(new HepInstruction.MatchOrder(order));
  }

  /**
   * Adds an instruction to limit the number of pattern matches for subsequent
   * instructions. The limit will take effect for the rest of the program (not
   * counting subprograms) or until another limit instruction is encountered.
   *
   * @param limit limit to set; use {@link HepProgram#MATCH_UNTIL_FIXPOINT} to
   *              remove limit
   */
  public HepProgramBuilder addMatchLimit(int limit) {
    checkArgument(group < 0);
    return addInstruction(new HepInstruction.MatchLimit(limit));
  }

  /**
   * Adds an instruction to execute a subprogram. Note that this is different
   * from adding the instructions from the subprogram individually. When added
   * as a subprogram, the sequence will execute repeatedly until a fixpoint is
   * reached, whereas when the instructions are added individually, the
   * sequence will only execute once (with a separate fixpoint for each
   * instruction).
   *
   * <p>The subprogram has its own state for match order and limit
   * (initialized to the defaults every time the subprogram is executed) and
   * any changes it makes to those settings do not affect the parent program.
   *
   * @param program subProgram to execute
   */
  public HepProgramBuilder addSubprogram(HepProgram program) {
    checkArgument(group < 0);
    return addInstruction(new HepInstruction.SubProgram(program));
  }

  private HepProgramBuilder addInstruction(HepInstruction instruction) {
    instructions.add(instruction);
    return this;
  }

  /**
   * Returns the constructed program, clearing the state of this program
   * builder as a side-effect.
   *
   * @return immutable program
   */
  public HepProgram build() {
    checkArgument(group < 0);
    HepProgram program = new HepProgram(instructions);
    clear();
    return program;
  }
}
