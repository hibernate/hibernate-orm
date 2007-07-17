package org.hibernate.test.entitymode.map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.entitymode.map.basic.DynamicClassTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class MapSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "map entity-mode suite");
		suite.addTest( DynamicClassTest.suite() );
		return suite;
	}
}
