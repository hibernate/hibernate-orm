package org.hibernate.test.component;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.component.basic.ComponentTest;
import org.hibernate.test.component.cascading.collection.CascadeToComponentCollectionTest;
import org.hibernate.test.component.cascading.toone.CascadeToComponentAssociationTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class ComponentSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "component test suite" );
		suite.addTest( ComponentTest.suite() );
		suite.addTest( CascadeToComponentCollectionTest.suite() );
		suite.addTest( CascadeToComponentAssociationTest.suite() );
		return suite;
	}
}
