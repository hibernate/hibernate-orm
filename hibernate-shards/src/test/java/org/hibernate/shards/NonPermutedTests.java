/**
 * Copyright (C) 2007 Google Inc.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package org.hibernate.shards;

import junit.framework.TestCase;

import org.hibernate.shards.util.Lists;

import java.util.Collections;
import java.util.List;

/**
 * Generated using NonPermutedTestsGenerator
 *
 * @author maxr@google.com (Max Ross)
 */
public final class NonPermutedTests {
  private NonPermutedTests() {}

  public static final List<Class<? extends TestCase>> CLASSES = Collections.unmodifiableList(buildListOfClasses());

  private static List<Class<? extends TestCase>> buildListOfClasses() {
    List<Class<? extends TestCase>> classes = Lists.newArrayList();

    // begin generated code
    classes.add(org.hibernate.shards.BaseHasShardIdListTest.class);
    classes.add(org.hibernate.shards.InstanceShardStrategyImplTest.class);
    classes.add(org.hibernate.shards.ShardImplTest.class);
    classes.add(org.hibernate.shards.ShardedConfigurationTest.class);
    classes.add(org.hibernate.shards.criteria.AddCriterionEventTest.class);
    classes.add(org.hibernate.shards.criteria.AddOrderEventTest.class);
    classes.add(org.hibernate.shards.criteria.CreateAliasEventTest.class);
    classes.add(org.hibernate.shards.criteria.CreateSubcriteriaEventTest.class);
    classes.add(org.hibernate.shards.criteria.CriteriaFactoryImplTest.class);
    classes.add(org.hibernate.shards.criteria.SetCacheModeEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetCacheRegionEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetCacheableEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetCommentEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetFetchModeEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetFetchSizeEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetFirstResultEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetFlushModeEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetLockModeEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetMaxResultsEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetProjectionEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetResultTransformerEventTest.class);
    classes.add(org.hibernate.shards.criteria.SetTimeoutEventTest.class);
    classes.add(org.hibernate.shards.criteria.ShardedSubcriteriaImplTest.class);
    classes.add(org.hibernate.shards.criteria.SubcriteriaFactoryImplTest.class);
    classes.add(org.hibernate.shards.id.ShardedTableHiLoGeneratorTest.class);
    classes.add(org.hibernate.shards.id.ShardedUUIDGeneratorTest.class);
    classes.add(org.hibernate.shards.integration.model.MemoryLeakTest.class);
    classes.add(org.hibernate.shards.integration.model.ModelIntegrationTest.class);
    classes.add(org.hibernate.shards.loadbalance.RoundRobinShardLoadBalancerTest.class);
    classes.add(org.hibernate.shards.query.SetBigDecimalEventTest.class);
    classes.add(org.hibernate.shards.query.SetBigIntegerEventTest.class);
    classes.add(org.hibernate.shards.query.SetBinaryEventTest.class);
    classes.add(org.hibernate.shards.query.SetBooleanEventTest.class);
    classes.add(org.hibernate.shards.query.SetByteEventTest.class);
    classes.add(org.hibernate.shards.query.SetCacheModeEventTest.class);
    classes.add(org.hibernate.shards.query.SetCacheRegionEventTest.class);
    classes.add(org.hibernate.shards.query.SetCacheableEventTest.class);
    classes.add(org.hibernate.shards.query.SetCalendarDateEventTest.class);
    classes.add(org.hibernate.shards.query.SetCalendarEventTest.class);
    classes.add(org.hibernate.shards.query.SetCharacterEventTest.class);
    classes.add(org.hibernate.shards.query.SetCommentEventTest.class);
    classes.add(org.hibernate.shards.query.SetDateEventTest.class);
    classes.add(org.hibernate.shards.query.SetDoubleEventTest.class);
    classes.add(org.hibernate.shards.query.SetEntityEventTest.class);
    classes.add(org.hibernate.shards.query.SetFetchSizeEventTest.class);
    classes.add(org.hibernate.shards.query.SetFirstResultEventTest.class);
    classes.add(org.hibernate.shards.query.SetFloatEventTest.class);
    classes.add(org.hibernate.shards.query.SetFlushModeEventTest.class);
    classes.add(org.hibernate.shards.query.SetIntegerEventTest.class);
    classes.add(org.hibernate.shards.query.SetLocaleEventTest.class);
    classes.add(org.hibernate.shards.query.SetLockModeEventTest.class);
    classes.add(org.hibernate.shards.query.SetLongEventTest.class);
    classes.add(org.hibernate.shards.query.SetMaxResultsEventTest.class);
    classes.add(org.hibernate.shards.query.SetParameterEventTest.class);
    classes.add(org.hibernate.shards.query.SetParameterListEventTest.class);
    classes.add(org.hibernate.shards.query.SetParametersEventTest.class);
    classes.add(org.hibernate.shards.query.SetPropertiesEventTest.class);
    classes.add(org.hibernate.shards.query.SetReadOnlyEventTest.class);
    classes.add(org.hibernate.shards.query.SetResultTransformerEventTest.class);
    classes.add(org.hibernate.shards.query.SetSerializableEventTest.class);
    classes.add(org.hibernate.shards.query.SetShortEventTest.class);
    classes.add(org.hibernate.shards.query.SetStringEventTest.class);
    classes.add(org.hibernate.shards.query.SetTextEventTest.class);
    classes.add(org.hibernate.shards.query.SetTimeEventTest.class);
    classes.add(org.hibernate.shards.query.SetTimeoutEventTest.class);
    classes.add(org.hibernate.shards.query.SetTimestampEventTest.class);
    classes.add(org.hibernate.shards.session.CrossShardRelationshipDetectingInterceptorDecoratorTest.class);
    classes.add(org.hibernate.shards.session.CrossShardRelationshipDetectingInterceptorTest.class);
    classes.add(org.hibernate.shards.session.DisableFilterOpenSessionEventTest.class);
    classes.add(org.hibernate.shards.session.EnableFilterOpenSessionEventTest.class);
    classes.add(org.hibernate.shards.session.SetCacheModeOpenSessionEventTest.class);
    classes.add(org.hibernate.shards.session.SetFlushModeOpenSessionEventTest.class);
    classes.add(org.hibernate.shards.session.SetReadOnlyOpenSessionEventTest.class);
    classes.add(org.hibernate.shards.session.SetSessionOnRequiresSessionEventTest.class);
    classes.add(org.hibernate.shards.session.ShardedSessionFactoryImplTest.class);
    classes.add(org.hibernate.shards.session.ShardedSessionImplTest.class);
    classes.add(org.hibernate.shards.strategy.access.ParallelShardAccessStrategyTest.class);
    classes.add(org.hibernate.shards.strategy.access.ParallelShardOperationCallableTest.class);
    classes.add(org.hibernate.shards.strategy.access.StartAwareFutureTaskTest.class);
    classes.add(org.hibernate.shards.strategy.exit.AggregateExitOperationTest.class);
    classes.add(org.hibernate.shards.strategy.exit.AvgResultsExitOperationTest.class);
    classes.add(org.hibernate.shards.strategy.exit.ExitOperationUtilsTest.class);
    classes.add(org.hibernate.shards.strategy.exit.FirstNonNullResultExitStrategyTest.class);
    classes.add(org.hibernate.shards.strategy.exit.FirstResultExitOperationTest.class);
    classes.add(org.hibernate.shards.strategy.exit.MaxResultExitOperationTest.class);
    classes.add(org.hibernate.shards.strategy.exit.OrderExitOperationTest.class);
    classes.add(org.hibernate.shards.strategy.exit.ProjectionExitOperationFactoryTest.class);
    classes.add(org.hibernate.shards.strategy.exit.RowCountExitOperationTest.class);
    classes.add(org.hibernate.shards.strategy.selection.LoadBalancedShardSelectionStrategyTest.class);
    classes.add(org.hibernate.shards.transaction.ShardedTransactionImplTest.class);

    // end generated code

    return classes;
  }

}
