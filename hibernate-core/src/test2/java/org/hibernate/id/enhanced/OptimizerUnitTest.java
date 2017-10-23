/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.enhanced;

import org.junit.Ignore;
import org.junit.Test;

import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class OptimizerUnitTest extends BaseUnitTestCase {
	@Test
	public void testBasicNoOptimizerUsage() {
		// test historic sequence behavior, where the initial values start at 1...
		SourceMock sequence = new SourceMock( 1 );
		Optimizer optimizer = buildNoneOptimizer( -1, 1 );
		for ( int i = 1; i < 11; i++ ) {
			final Long next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 10, sequence.getTimesCalled() );
		assertEquals( 10, sequence.getCurrentValue() );

		// test historic table behavior, where the initial values started at 0 (we now force 1 to be the first used id value)
		sequence = new SourceMock( 0 );
		optimizer = buildNoneOptimizer( -1, 1 );
		for ( int i = 1; i < 11; i++ ) {
			final Long next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 11, sequence.getTimesCalled() ); // an extra time to get to 1 initially
		assertEquals( 10, sequence.getCurrentValue() );
	}
	@Test
	public void testBasicNoOptimizerUsageWithNegativeValues() {
		// test historic sequence behavior, where the initial values start at 1...
		SourceMock sequence = new SourceMock( -1, -1 );
		Optimizer optimizer = buildNoneOptimizer( -1, -1 );
		for ( int i = 1; i < 11; i++ ) {
			final Long next = ( Long ) optimizer.generate( sequence );
			assertEquals( -i, next.intValue() );
		}
		assertEquals( 10, sequence.getTimesCalled() );
		assertEquals( -10, sequence.getCurrentValue() );

		// test historic table behavior, where the initial values started at 0 (we now force 1 to be the first used id value)
		sequence = new SourceMock( 0 );
		optimizer = buildNoneOptimizer( -1, 1 );
		for ( int i = 1; i < 11; i++ ) {
			final Long next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 11, sequence.getTimesCalled() ); // an extra time to get to 1 initially
		assertEquals( 10, sequence.getCurrentValue() );
	}

	@Test
	public void testBasicHiLoOptimizerUsage() {
		int increment = 10;
		Long next;

		// test historic sequence behavior, where the initial values start at 1...
		SourceMock sequence = new SourceMock( 1 );
		Optimizer optimizer = buildHiloOptimizer( -1, increment );
		for ( int i = 1; i <= increment; i++ ) {
			next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 1, sequence.getTimesCalled() ); // once to initialze state
		assertEquals( 1, sequence.getCurrentValue() );
		// force a "clock over"
		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 11, next.intValue() );
		assertEquals( 2, sequence.getTimesCalled() );
		assertEquals( 2, sequence.getCurrentValue() );

		// test historic table behavior, where the initial values started at 0 (we now force 1 to be the first used id value)
		sequence = new SourceMock( 0 );
		optimizer = buildHiloOptimizer( -1, increment );
		for ( int i = 1; i <= increment; i++ ) {
			next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 2, sequence.getTimesCalled() ); // here have have an extra call to get to 1 initially
		assertEquals( 1, sequence.getCurrentValue() );
		// force a "clock over"
		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 11, next.intValue() );
		assertEquals( 3, sequence.getTimesCalled() );
		assertEquals( 2, sequence.getCurrentValue() );
	}

	@Test
	public void testBasicPooledOptimizerUsage() {
		Long next;
		// test historic sequence behavior, where the initial values start at 1...
		SourceMock sequence = new SourceMock( 1, 10 );
		Optimizer optimizer = buildPooledOptimizer( -1, 10 );
		for ( int i = 1; i <= 11; i++ ) {
			next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 2, sequence.getTimesCalled() ); // twice to initialize state
		assertEquals( 11, sequence.getCurrentValue() );
		// force a "clock over"
		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 12, next.intValue() );
		assertEquals( 3, sequence.getTimesCalled() );
		assertEquals( 21, sequence.getCurrentValue() );
	}

	@Test
	public void testSubsequentPooledOptimizerUsage() {
		// test the pooled optimizer in situation where the sequence is already beyond its initial value on init.
		//		cheat by telling the sequence to start with 1000
		final SourceMock sequence = new SourceMock( 1001, 3, 5 );
		//		but tell the optimizer the start-with is 1
		final Optimizer optimizer = buildPooledOptimizer( 1, 3 );

		assertEquals( 5, sequence.getTimesCalled() );
		assertEquals( 1001, sequence.getCurrentValue() );

		Long next = (Long) optimizer.generate( sequence );
		assertEquals( 1001 +1 , next.intValue() );
		assertEquals( (5+1), sequence.getTimesCalled() );
		assertEquals( (1001+3), sequence.getCurrentValue() );

		next = (Long) optimizer.generate( sequence );
		assertEquals( (1001+2), next.intValue() );
		assertEquals( (5+1), sequence.getTimesCalled() );
		assertEquals( (1001+3), sequence.getCurrentValue() );

		next = (Long) optimizer.generate( sequence );
		assertEquals( (1001+3), next.intValue() );
		assertEquals( (5+1), sequence.getTimesCalled() );
		assertEquals( (1001+3), sequence.getCurrentValue() );

		// force a "clock over"
		next = (Long) optimizer.generate( sequence );
		assertEquals( (1001+4), next.intValue() );
		assertEquals( (5+2), sequence.getTimesCalled() );
		assertEquals( (1001+6), sequence.getCurrentValue() );
	}

	@Test
	public void testBasicPooledLoOptimizerUsage() {
		final SourceMock sequence = new SourceMock( 1, 3 );
		final Optimizer optimizer = buildPooledLoOptimizer( 1, 3 );

		assertEquals( 0, sequence.getTimesCalled() );
		assertEquals( -1, sequence.getCurrentValue() );

		Long next = ( Long ) optimizer.generate( sequence );
		assertEquals( 1, next.intValue() );
		assertEquals( 1, sequence.getTimesCalled() );
		assertEquals( 1, sequence.getCurrentValue() );

		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 2, next.intValue() );
		assertEquals( 1, sequence.getTimesCalled() );
		assertEquals( 1, sequence.getCurrentValue() );

		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 3, next.intValue() );
		assertEquals( 1, sequence.getTimesCalled() );
		assertEquals( 1, sequence.getCurrentValue() );

//		// force a "clock over"
		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 4, next.intValue() );
		assertEquals( 2, sequence.getTimesCalled() );
		assertEquals( (1+3), sequence.getCurrentValue() );
	}

	@Test
	public void testSubsequentPooledLoOptimizerUsage() {
		// test the pooled-lo optimizer in situation where the sequence is already beyond its initial value on init.
		//		cheat by telling the sequence to start with 1000
		final SourceMock sequence = new SourceMock( 1001, 3, 5 );
		//		but tell the optimizer the start-with is 1
		final Optimizer optimizer = buildPooledLoOptimizer( 1, 3 );

		assertEquals( 5, sequence.getTimesCalled() );
		assertEquals( 1001, sequence.getCurrentValue() );

		// should "clock over" immediately
		Long next = ( Long ) optimizer.generate( sequence );
		assertEquals( (1001+3), next.intValue() );
		assertEquals( (5+1), sequence.getTimesCalled() );
		assertEquals( (1001+3), sequence.getCurrentValue() );

		next = ( Long ) optimizer.generate( sequence );
		assertEquals( (1001+4), next.intValue() );
		assertEquals( (5+1), sequence.getTimesCalled() );
		assertEquals( (1001+3), sequence.getCurrentValue() );

		next = ( Long ) optimizer.generate( sequence );
		assertEquals( (1001+5), next.intValue() );
		assertEquals( (5+1), sequence.getTimesCalled() );
		assertEquals( (1001+3), sequence.getCurrentValue() );

//		// force a "clock over"
		next = ( Long ) optimizer.generate( sequence );
		assertEquals( (1001+6), next.intValue() );
		assertEquals( (5+2), sequence.getTimesCalled() );
		assertEquals( (1001+6), sequence.getCurrentValue() );
	}

	@Test
	public void testRecoveredPooledOptimizerUsage() {
		final SourceMock sequence = new SourceMock( 1, 3 );
		final Optimizer optimizer = buildPooledOptimizer( 1, 3 );

		assertEquals( 0, sequence.getTimesCalled() );
		assertEquals( -1, sequence.getCurrentValue() );

		Long next = ( Long ) optimizer.generate( sequence );
		assertEquals( 1, next.intValue() );
		assertEquals( 2, sequence.getTimesCalled() );
		assertEquals( 4, sequence.getCurrentValue() );

		// app ends, and starts back up (we should "lose" only 2 and 3 as id values)
		final Optimizer optimizer2 = buildPooledOptimizer( 1, 3 );
		next = ( Long ) optimizer2.generate( sequence );
		assertEquals( 5, next.intValue() );
		assertEquals( 3, sequence.getTimesCalled() );
		assertEquals( 7, sequence.getCurrentValue() );
	}
	@Test
	public void testRecoveredPooledLoOptimizerUsage() {
		final SourceMock sequence = new SourceMock( 1, 3 );
		final Optimizer optimizer = buildPooledLoOptimizer( 1, 3 );

		assertEquals( 0, sequence.getTimesCalled() );
		assertEquals( -1, sequence.getCurrentValue() );

		Long next = ( Long ) optimizer.generate( sequence );
		assertEquals( 1, next.intValue() );
		assertEquals( 1, sequence.getTimesCalled() );
		assertEquals( 1, sequence.getCurrentValue() );

		// app ends, and starts back up (we should "lose" only 2 and 3 as id values)
		final Optimizer optimizer2 = buildPooledLoOptimizer( 1, 3 );
		next = ( Long ) optimizer2.generate( sequence );
		assertEquals( 4, next.intValue() );
		assertEquals( 2, sequence.getTimesCalled() );
		assertEquals( 4, sequence.getCurrentValue() );
	}

	@Test
	public void testBasicPooledThreadLocalLoOptimizerUsage() {
		final SourceMock sequence = new SourceMock( 1, 50 ); // pass 5000 to match default for PooledThreadLocalLoOptimizer.THREAD_LOCAL_BLOCK_SIZE
		final Optimizer optimizer = buildPooledThreadLocalLoOptimizer( 1, 50 );

		assertEquals( 0, sequence.getTimesCalled() );
		assertEquals( -1, sequence.getCurrentValue() );

		Long next = ( Long ) optimizer.generate( sequence );
		assertEquals( 1, next.intValue() );
		assertEquals( 1, sequence.getTimesCalled() );
		assertEquals( 1, sequence.getCurrentValue() );

		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 2, next.intValue() );
		assertEquals( 1, sequence.getTimesCalled() );
		assertEquals( 1, sequence.getCurrentValue() );

		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 3, next.intValue() );
		assertEquals( 1, sequence.getTimesCalled() );
		assertEquals( 1, sequence.getCurrentValue() );

		for( int looper = 0; looper < 51; looper++) {
			next = ( Long ) optimizer.generate( sequence );
		}

		assertEquals( 3 + 51, next.intValue() );
		assertEquals( 2, sequence.getTimesCalled() );
		assertEquals( 51, sequence.getCurrentValue() );

	}

	private static Optimizer buildNoneOptimizer(long initial, int increment) {
		return buildOptimizer( StandardOptimizerDescriptor.NONE, initial, increment );
	}

	private static Optimizer buildHiloOptimizer(long initial, int increment) {
		return buildOptimizer( StandardOptimizerDescriptor.HILO, initial, increment );
	}

	private static Optimizer buildPooledOptimizer(long initial, int increment) {
		return buildOptimizer( StandardOptimizerDescriptor.POOLED, initial, increment );
	}

	private static Optimizer buildPooledLoOptimizer(long initial, int increment) {
		return buildOptimizer( StandardOptimizerDescriptor.POOLED_LO, initial, increment );
	}

	private static Optimizer buildPooledThreadLocalLoOptimizer(long initial, int increment) {
		return buildOptimizer( StandardOptimizerDescriptor.POOLED_LOTL, initial, increment );
	}

	private static Optimizer buildOptimizer(
			StandardOptimizerDescriptor descriptor,
			long initial,
			int increment) {
		return OptimizerFactory.buildOptimizer( descriptor.getExternalName(), Long.class, increment, initial );
	}

	private static class SourceMock implements AccessCallback {
		private IdentifierGeneratorHelper.BasicHolder value = new IdentifierGeneratorHelper.BasicHolder( Long.class );
		private long initialValue;
		private int increment;
		private int timesCalled = 0;

		public SourceMock(long initialValue) {
			this( initialValue, 1 );
		}

		public SourceMock(long initialValue, int increment) {
			this( initialValue, increment, 0 );
		}

		public SourceMock(long initialValue, int increment, int timesCalled) {
			this.increment = increment;
			this.timesCalled = timesCalled;
			if ( timesCalled != 0 ) {
				this.value.initialize( initialValue );
				this.initialValue = 1;
			}
			else {
				this.value.initialize( -1 );
				this.initialValue = initialValue;
			}
		}

		public IntegralDataTypeHolder getNextValue() {
			try {
				if ( timesCalled == 0 ) {
					initValue();
					return value.copy();
				}
				else {
					return value.add( increment ).copy();
				}
			}
			finally {
				timesCalled++;
			}
		}

		@Override
		public String getTenantIdentifier() {
			return null;
		}

		private void initValue() {
			this.value.initialize( initialValue );
		}

		public int getTimesCalled() {
			return timesCalled;
		}

		public long getCurrentValue() {
			return value == null ? -1 : value.getActualLongValue();
		}
	}

}
