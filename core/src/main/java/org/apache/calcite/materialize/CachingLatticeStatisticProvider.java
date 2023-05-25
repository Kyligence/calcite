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

import org.apache.calcite.util.Util;

import org.apache.kylin.guava30.shaded.common.cache.CacheBuilder;
import org.apache.kylin.guava30.shaded.common.cache.CacheLoader;
import org.apache.kylin.guava30.shaded.common.cache.LoadingCache;
import org.apache.kylin.guava30.shaded.common.collect.ImmutableList;
import org.apache.kylin.guava30.shaded.common.util.concurrent.UncheckedExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Implementation of {@link LatticeStatisticProvider} that caches single-column
 * statistics and computes multi-column statistics from these.
 */
class CachingLatticeStatisticProvider implements LatticeStatisticProvider {
  private final Lattice lattice;
  private final LoadingCache<Lattice.Column, Double> cache;

  /** Creates a CachingStatisticProvider. */
  CachingLatticeStatisticProvider(final Lattice lattice,
      final LatticeStatisticProvider provider) {
    this.lattice = lattice;
    cache = CacheBuilder.newBuilder().build(
        CacheLoader.from(key -> provider.cardinality(ImmutableList.of(key))));
  }

  @Override public double cardinality(List<Lattice.Column> columns) {
    final List<Double> counts = new ArrayList<>();
    for (Lattice.Column column : columns) {
      try {
        counts.add(cache.get(column));
      } catch (UncheckedExecutionException | ExecutionException e) {
        throw Util.throwAsRuntime(Util.causeOrSelf(e));
      }
    }
    return (int) Lattice.getRowCount(lattice.getFactRowCount(), counts);
  }
}
