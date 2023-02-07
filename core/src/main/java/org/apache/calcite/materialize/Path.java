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
package org.apache.calcite.materialize;

import io.kyligence.kap.guava20.shaded.common.collect.ImmutableList;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/** A sequence of {@link Step}s from a root node (fact table) to another node
 * (dimension table), possibly via intermediate dimension tables. */
class Path {
  final List<Step> steps;
  private final int id;

  Path(List<Step> steps, int id) {
    this.steps = ImmutableList.copyOf(steps);
    this.id = id;
  }

  @Override public int hashCode() {
    return id;
  }

  @Override public boolean equals(@Nullable Object obj) {
    return this == obj
        || obj instanceof Path
        && id == ((Path) obj).id;
  }
}
