package org.hibernate.test.entitymode;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.entitymode.dom4j.Dom4jSuite;
import org.hibernate.test.entitymode.map.MapSuite;
import org.hibernate.test.entitymode.multi.MultiRepresentationTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class EntityModeSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "entity-mode tests" );
		suite.addTest( Dom4jSuite.suite() );
		suite.addTest( MapSuite.suite() );
		suite.addTest( MultiRepresentationTest.suite() );
		return suite;
	}
}
