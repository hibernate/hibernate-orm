package org.hibernate.orm.test.id.enhanced;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link Optimizer#generateBatch} yields exactly the same ids as the
 * equivalent number of singular {@link Optimizer#generate} calls, for any
 * partitioning of the requested ids into batches, and for source values that
 * start at zero or below.
 */
class OptimizerBatchGenerationTest {

	private static final int[] INCREMENT_SIZES = { 3, 50 };

	private static final int[][] BATCH_PLANS = {
			{ 1, 1, 1 },
			{ 3, 3, 3 },
			{ 2, 5, 1 },
			{ 10 },
			{ 50, 1 },
			{ 7, 100 },
			{ 101 },
	};

	/**
	 * Hands out sequential source values: start, start + step, ...
	 */
	private static class Source implements AccessCallback {
		private long next;
		private final int step;

		Source(long start, int step) {
			this.next = start;
			this.step = step;
		}

		@Override
		public long getNextValue() {
			final long value = next;
			next += step;
			return value;
		}

		@Override
		public String getTenantIdentifier() {
			return null;
		}
	}

	static Stream<Arguments> scenarios() {
		final List<Arguments> scenarios = new ArrayList<>();
		for ( StandardOptimizerDescriptor descriptor : StandardOptimizerDescriptor.values() ) {
			for ( int incrementSize : INCREMENT_SIZES ) {
				final boolean appliesIncrement = optimizer( descriptor, incrementSize ).applyIncrementSizeToSourceValues();
				final int step = appliesIncrement ? incrementSize : 1;
				// First source value: 0 and 1 for all, and values below 1 where the increment is applied
				final long[] starts = appliesIncrement
						? new long[] { 1, 0, -incrementSize }
						: new long[] { 1, 0 };
				for ( long start : starts ) {
					for ( int[] plan : BATCH_PLANS ) {
						scenarios.add( Arguments.of( descriptor, incrementSize, step, start, plan ) );
					}
				}
			}
		}
		return scenarios.stream();
	}

	private static Optimizer optimizer(StandardOptimizerDescriptor descriptor, int incrementSize) {
		return OptimizerFactory.buildOptimizer( descriptor, Long.class, incrementSize, 1 );
	}

	@ParameterizedTest(name = "{0}, increment {1}, source start {3}, batches {4}")
	@MethodSource("scenarios")
	void batchMatchesSingularGeneration(
			StandardOptimizerDescriptor descriptor,
			int incrementSize,
			int step,
			long start,
			int[] plan) {
		final int total = java.util.Arrays.stream( plan ).sum();

		final Optimizer single = optimizer( descriptor, incrementSize );
		final Source singleSource = new Source( start, step );
		final List<Serializable> expected = new ArrayList<>( total );
		for ( int i = 0; i < total; i++ ) {
			expected.add( single.generate( singleSource ) );
		}

		final Optimizer batch = optimizer( descriptor, incrementSize );
		final Source batchSource = new Source( start, step );
		final List<Serializable> actual = new ArrayList<>( total );
		for ( int count : plan ) {
			final Serializable[] ids = batch.generateBatch( count, batchSource );
			assertThat( ids ).as( "batch of %s", count ).hasSize( count );
			actual.addAll( List.of( ids ) );
		}

		assertThat( actual ).containsExactlyElementsOf( expected );
	}

	/**
	 * Batches and singular calls can be interleaved on the same optimizer without repeating ids.
	 */
	@ParameterizedTest(name = "{0}, increment {1}")
	@MethodSource("interleavedScenarios")
	void interleavedBatchAndSingularGeneration(StandardOptimizerDescriptor descriptor, int incrementSize) {
		final Optimizer optimizer = optimizer( descriptor, incrementSize );
		final int step = optimizer.applyIncrementSizeToSourceValues() ? incrementSize : 1;
		final Source source = new Source( 1, step );
		final List<Serializable> ids = new ArrayList<>();
		for ( int round = 0; round < 10; round++ ) {
			ids.add( optimizer.generate( source ) );
			ids.addAll( List.of( optimizer.generateBatch( incrementSize, source ) ) );
			ids.add( optimizer.generate( source ) );
		}
		assertThat( ids ).doesNotHaveDuplicates();
	}

	static Stream<Arguments> interleavedScenarios() {
		final List<Arguments> scenarios = new ArrayList<>();
		for ( StandardOptimizerDescriptor descriptor : StandardOptimizerDescriptor.values() ) {
			for ( int incrementSize : INCREMENT_SIZES ) {
				scenarios.add( Arguments.of( descriptor, incrementSize ) );
			}
		}
		return scenarios.stream();
	}
}
