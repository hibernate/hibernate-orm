package org.hibernate.test.propertyref;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.propertyref.basic.PropertyRefTest;
import org.hibernate.test.propertyref.component.complete.CompleteComponentPropertyRefTest;
import org.hibernate.test.propertyref.component.partial.PartialComponentPropertyRefTest;
import org.hibernate.test.propertyref.inheritence.discrim.SubclassPropertyRefTest;
import org.hibernate.test.propertyref.inheritence.joined.JoinedSubclassPropertyRefTest;
import org.hibernate.test.propertyref.inheritence.union.UnionSubclassPropertyRefTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class PropertyRefSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "property-ref suite" );
		suite.addTest( PropertyRefTest.suite() );
		suite.addTest( CompleteComponentPropertyRefTest.suite() );
		suite.addTest( PartialComponentPropertyRefTest.suite() );
		suite.addTest( SubclassPropertyRefTest.suite() );
		suite.addTest( JoinedSubclassPropertyRefTest.suite() );
		suite.addTest( UnionSubclassPropertyRefTest.suite() );
		return suite;
	}
}
