package org.hibernate.test.idgen.enhanced;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.junit.UnitTestCase;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.AccessCallback;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class OptimizerUnitTest extends UnitTestCase {
	public OptimizerUnitTest(String string) {
		super( string );
	}

	public static Test suite() {
		return new TestSuite( OptimizerUnitTest.class );
	}

	public void testBasicNoOptimizerUsage() {
		// test historic sequence behavior, where the initial values start at 1...
		SourceMock sequence = new SourceMock( 1 );
		Optimizer optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.NONE, Long.class, 1 );
		for ( int i = 1; i < 11; i++ ) {
			final Long next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 10, sequence.getTimesCalled() );
		assertEquals( 10, sequence.getCurrentValue() );

		// test historic table behavior, where the initial values started at 0 (we now force 1 to be the first used id value)
		sequence = new SourceMock( 0 );
		optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.NONE, Long.class, 1 );
		for ( int i = 1; i < 11; i++ ) {
			final Long next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 11, sequence.getTimesCalled() ); // an extra time to get to 1 initially
		assertEquals( 10, sequence.getCurrentValue() );
	}

	public void testBasicHiLoOptimizerUsage() {
		int increment = 10;
		Long next;

		// test historic sequence behavior, where the initial values start at 1...
		SourceMock sequence = new SourceMock( 1 );
		Optimizer optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.HILO, Long.class, increment );
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
		optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.HILO, Long.class, increment );
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

	public void testBasicPooledOptimizerUsage() {
		Long next;
		// test historic sequence behavior, where the initial values start at 1...
		SourceMock sequence = new SourceMock( 1, 10 );
		Optimizer optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.POOL, Long.class, 10 );
		for ( int i = 1; i < 11; i++ ) {
			next = ( Long ) optimizer.generate( sequence );
			assertEquals( i, next.intValue() );
		}
		assertEquals( 2, sequence.getTimesCalled() ); // twice to initialze state
		assertEquals( 11, sequence.getCurrentValue() );
		// force a "clock over"
		next = ( Long ) optimizer.generate( sequence );
		assertEquals( 11, next.intValue() );
		assertEquals( 3, sequence.getTimesCalled() );
		assertEquals( 21, sequence.getCurrentValue() );
	}

	private static class SourceMock implements AccessCallback {
		private long value;
		private int increment;
		private int timesCalled = 0;

		public SourceMock(long initialValue) {
			this( initialValue, 1 );
		}

		public SourceMock(long initialValue, int increment) {
			this.increment = increment;
			this.value = initialValue - increment;
		}

		public long getNextValue() {
			timesCalled++;
			return ( value += increment );
		}

		public int getTimesCalled() {
			return timesCalled;
		}

		public long getCurrentValue() {
			return value;
		}
	}

//	public void testNoopDumping() {
//		SourceMock sequence = new SourceMock( 1 );
//		Optimizer optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.NONE, Long.class, 1 );
//		for ( int i = 1; i <= 41; i++ ) {
//			System.out.println( i + " => " + optimizer.generate( sequence ) + " (" + sequence.getCurrentValue() + ")" );
//		}
//	}
//
//	public void testHiLoDumping() {
//		int increment = 10;
//		SourceMock sequence = new SourceMock( 1 );
//		Optimizer optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.HILO, Long.class, increment );
//		for ( int i = 1; i <= 41; i++ ) {
//			System.out.println( i + " => " + optimizer.generate( sequence ) + " (" + sequence.getCurrentValue() + ")" );
//		}
//	}
//
//	public void testPooledDumping() {
//		int increment = 10;
//		SourceMock sequence = new SourceMock( 1, increment );
//		Optimizer optimizer = OptimizerFactory.buildOptimizer( OptimizerFactory.POOL, Long.class, increment );
//		for ( int i = 1; i <= 41; i++ ) {
//			System.out.println( i + " => " + optimizer.generate( sequence ) + " (" + sequence.getCurrentValue() + ")" );
//		}
//	}

}
