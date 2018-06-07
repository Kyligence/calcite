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
package org.apache.calcite.sql2rel;

import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.RelFactories;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.util.ReflectiveVisitor;

/*
 * OVERRIDE POINT:
 * - disable the whole RelFieldTrimmer
 */

/**
 * Transformer that walks over a tree of relational expressions, replacing each
 * {@link RelNode} with a 'slimmed down' relational expression that projects
 * only the columns required by its consumer.
 *
 * <p>Uses multi-methods to fire the right rule for each type of relational
 * expression. This allows the transformer to be extended without having to
 * add a new method to RelNode, and without requiring a collection of rule
 * classes scattered to the four winds.
 *
 * <p>REVIEW: jhyde, 2009/7/28: Is sql2rel the correct package for this class?
 * Trimming fields is not an essential part of SQL-to-Rel translation, and
 * arguably belongs in the optimization phase. But this transformer does not
 * obey the usual pattern for planner rules; it is difficult to do so, because
 * each {@link RelNode} needs to return a different set of fields after
 * trimming.
 *
 * <p>TODO: Change 2nd arg of the {@link #trimFields} method from BitSet to
 * Mapping. Sometimes it helps the consumer if you return the columns in a
 * particular order. For instance, it may avoid a project at the top of the
 * tree just for reordering. Could ease the transition by writing methods that
 * convert BitSet to Mapping and vice versa.
 */
public class RelFieldTrimmer implements ReflectiveVisitor {
  public RelFieldTrimmer(SqlValidator validator, RelBuilder relBuilder) {
  }

  public RelFieldTrimmer(SqlValidator validator, RelOptCluster cluster,
           RelFactories.ProjectFactory projectFactory, RelFactories.FilterFactory filterFactory,
           RelFactories.JoinFactory joinFactory, RelFactories.SemiJoinFactory semiJoinFactory,
           RelFactories.SortFactory sortFactory, RelFactories.AggregateFactory aggregateFactory,
           RelFactories.SetOpFactory setOpFactory) {
  }

  public RelNode trim(RelNode rootRel) {
    return rootRel;
  }
}

// End RelFieldTrimmer.java
