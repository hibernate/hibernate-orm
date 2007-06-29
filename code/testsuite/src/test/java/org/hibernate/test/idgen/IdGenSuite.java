package org.hibernate.test.idgen;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.idgen.enhanced.OptimizerUnitTest;
import org.hibernate.test.idgen.enhanced.SequenceStyleConfigUnitTest;
import org.hibernate.test.idgen.enhanced.forcedtable.BasicForcedTableSequenceTest;
import org.hibernate.test.idgen.enhanced.forcedtable.HiLoForcedTableSequenceTest;
import org.hibernate.test.idgen.enhanced.forcedtable.PooledForcedTableSequenceTest;
import org.hibernate.test.idgen.enhanced.sequence.BasicSequenceTest;
import org.hibernate.test.idgen.enhanced.sequence.HiLoSequenceTest;
import org.hibernate.test.idgen.enhanced.sequence.PooledSequenceTest;
import org.hibernate.test.idgen.enhanced.table.BasicTableTest;
import org.hibernate.test.idgen.enhanced.table.HiLoTableTest;
import org.hibernate.test.idgen.enhanced.table.PooledTableTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class IdGenSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "enhanced id generators" );

		suite.addTest( OptimizerUnitTest.suite() );
		suite.addTest( SequenceStyleConfigUnitTest.suite() );

		suite.addTest( BasicForcedTableSequenceTest.suite() );
		suite.addTest( HiLoForcedTableSequenceTest.suite() );
		suite.addTest( PooledForcedTableSequenceTest.suite() );

		suite.addTest( BasicSequenceTest.suite() );
		suite.addTest( HiLoSequenceTest.suite() );
		suite.addTest( PooledSequenceTest.suite() );

		suite.addTest( BasicTableTest.suite() );
		suite.addTest( HiLoTableTest.suite() );
		suite.addTest( PooledTableTest.suite() );

		return suite;
	}
}
