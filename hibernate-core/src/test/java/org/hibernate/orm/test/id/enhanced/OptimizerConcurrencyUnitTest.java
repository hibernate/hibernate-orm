/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.enhanced;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.hibernate.AssertionFailure;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.Assert.assertEquals;

public class OptimizerConcurrencyUnitTest {

	@ParameterizedTest
	@MethodSource("org.hibernate.id.enhanced.StandardOptimizerDescriptor#values")
	public void testConcurrentUsage_singleTenancy(StandardOptimizerDescriptor descriptor) throws InterruptedException {
		final int increment = 50;
		final int taskCount = 100 * increment;

		Optimizer optimizer = buildOptimizer( 1, increment, descriptor );

		List<Callable<Long>> tasks = new ArrayList<>();

		SourceMock sequence = new SourceMock( 1, increment );
		Assertions.assertEquals( 0, sequence.getTimesCalled() );
		Assertions.assertEquals( -1, sequence.getCurrentValue() );

		for ( int i = 0; i < taskCount; i++ ) {
			tasks.add( () -> ( Long ) optimizer.generate( sequence ) );
		}

		ExecutorService executor = Executors.newFixedThreadPool( 10 );
		List<Future<Long>> futures;
		try {
			futures = executor.invokeAll( tasks );
			executor.shutdown();
			executor.awaitTermination( 10, TimeUnit.SECONDS );
		}
		finally {
			executor.shutdownNow();
		}

		assertThat( futures )
				.allSatisfy( future -> {
					assertThat( future ).isDone();
					assertThatCode( future::get ).doesNotThrowAnyException();
				} );
		List<Long> generated = futures.stream().map( this::getDoneFutureValue ).collect( Collectors.toList());
		assertThat( generated )
				.hasSize( taskCount )
				// Check for uniqueness
				.containsExactlyInAnyOrderElementsOf( new HashSet<>( generated ) );
		System.out.println( "Generated IDs: " + generated );
	}

	@ParameterizedTest
	@MethodSource("org.hibernate.id.enhanced.StandardOptimizerDescriptor#values")
	public void testConcurrentUsage_multiTenancy(StandardOptimizerDescriptor descriptor) throws InterruptedException {
		final int increment = 50;

		final int tenantCount = 5;
		final int taskCountPerTenant = 20 * increment;

		Optimizer optimizer = buildOptimizer( 1, increment, descriptor );

		Map<String, List<Callable<Long>>> tasksByTenantId = new LinkedHashMap<>();

		for ( int i = 0; i < tenantCount; i++ ) {
			String tenantId = "tenant#" + i;

			SourceMock sequenceForTenant = new SourceMock( tenantId, 1, increment );
			assertEquals( 0, sequenceForTenant.getTimesCalled() );
			assertEquals( -1, sequenceForTenant.getCurrentValue() );

			List<Callable<Long>> tasksForTenant = new ArrayList<>();
			tasksByTenantId.put( tenantId, tasksForTenant );
			for ( int j = 0; j < taskCountPerTenant; j++ ) {
				tasksForTenant.add( () -> ( Long ) optimizer.generate( sequenceForTenant ) );
			}
		}

		List<Callable<Long>> tasks = new ArrayList<>();
		// Make sure to interleave tenants
		for ( int i = 0; i < taskCountPerTenant; i++ ) {
			for ( List<Callable<Long>> tasksForTenant : tasksByTenantId.values() ) {
				tasks.add( tasksForTenant.get( i ) );
			}
		}

		ExecutorService executor = Executors.newFixedThreadPool( 10 );
		List<Future<Long>> futures;
		try {
			futures = executor.invokeAll( tasks );
			executor.shutdown();
			executor.awaitTermination( 10, TimeUnit.SECONDS );
		}
		finally {
			executor.shutdownNow();
		}

		assertThat( futures )
				.allSatisfy( future -> {
					assertThat( future ).isDone();
					assertThatCode( future::get ).doesNotThrowAnyException();
				} );

		Map<String, List<Future<Long>>> futuresByTenantId = new LinkedHashMap<>();
		for ( int i = 0; i < tenantCount; i++ ) {
			List<Future<Long>> futuresForTenant = new ArrayList<>();
			for ( int j = 0; j < taskCountPerTenant; j++ ) {
				futuresForTenant.add( futures.get( i + j * tenantCount ) );
			}
			String tenantId = "tenant#" + i;
			futuresByTenantId.put( tenantId, futuresForTenant );
		}

		for ( Map.Entry<String, List<Future<Long>>> entry : futuresByTenantId.entrySet() ) {
			List<Long> generated = entry.getValue().stream().map( this::getDoneFutureValue )
					.collect( Collectors.toList());
			assertThat( generated )
					.hasSize( taskCountPerTenant )
					// Check for uniqueness
					.containsExactlyInAnyOrderElementsOf( new HashSet<>( generated ) );
			System.out.println( "Generated IDs for '" + entry.getKey() + "': " + generated );
		}
	}

	private Optimizer buildOptimizer(long initial, int increment, StandardOptimizerDescriptor optimizerDescriptor) {
		return OptimizerFactory.buildOptimizer( optimizerDescriptor, Long.class, increment, initial );
	}

	private <R> R getDoneFutureValue(Future<R> future) {
		try {
			return future.get(0, TimeUnit.SECONDS);
		}
		catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new AssertionFailure( "Unexpected Future state", e );
		}
	}

}
