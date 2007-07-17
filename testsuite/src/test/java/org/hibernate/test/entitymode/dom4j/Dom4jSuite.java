package org.hibernate.test.entitymode.dom4j;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.entitymode.dom4j.accessors.Dom4jAccessorTest;
import org.hibernate.test.entitymode.dom4j.basic.Dom4jTest;
import org.hibernate.test.entitymode.dom4j.many2one.Dom4jManyToOneTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class Dom4jSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "dom4j entity-mode suite" );
		suite.addTest( Dom4jAccessorTest.suite() );
		suite.addTest( Dom4jTest.suite() );
		suite.addTest( Dom4jManyToOneTest.suite() );
		return suite;
	}
}
