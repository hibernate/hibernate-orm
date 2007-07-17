package org.hibernate.test.keymanytoone;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.keymanytoone.bidir.embedded.KeyManyToOneTest;
import org.hibernate.test.keymanytoone.bidir.component.LazyKeyManyToOneTest;
import org.hibernate.test.keymanytoone.bidir.component.EagerKeyManyToOneTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class KeyManyToOneSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "key-many-to-one mappings" );
		suite.addTest( KeyManyToOneTest.suite() );
		suite.addTest( LazyKeyManyToOneTest.suite() );
		suite.addTest( EagerKeyManyToOneTest.suite() );
		return suite;
	}
}
