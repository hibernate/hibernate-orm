package org.hibernate.test.generatedkeys;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.generatedkeys.identity.IdentityGeneratedKeysTest;
import org.hibernate.test.generatedkeys.select.SelectGeneratorTest;
import org.hibernate.test.generatedkeys.seqidentity.SequenceIdentityTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class GeneratedKeysSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "generated keys suite" );
		suite.addTest( IdentityGeneratedKeysTest.suite() );
		suite.addTest( SelectGeneratorTest.suite() );
		suite.addTest( SequenceIdentityTest.suite() );
		return suite;
	}
}
