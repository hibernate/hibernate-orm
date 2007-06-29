package org.hibernate.test.onetoone;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.hibernate.test.onetoone.formula.OneToOneFormulaTest;
import org.hibernate.test.onetoone.joined.JoinedSubclassOneToOneTest;
import org.hibernate.test.onetoone.link.OneToOneLinkTest;
import org.hibernate.test.onetoone.nopojo.DynamicMapOneToOneTest;
import org.hibernate.test.onetoone.singletable.DiscrimSubclassOneToOneTest;
import org.hibernate.test.onetoone.optional.OptionalOneToOneTest;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class OneToOneSuite {
	public static Test suite() {
		TestSuite suite = new TestSuite( "one-to-one suite" );
		suite.addTest( OneToOneFormulaTest.suite() );
		suite.addTest( JoinedSubclassOneToOneTest.suite() );
		suite.addTest( OneToOneLinkTest.suite() );
		suite.addTest( DynamicMapOneToOneTest.suite() );
		suite.addTest( OptionalOneToOneTest.suite() );
		suite.addTest( DiscrimSubclassOneToOneTest.suite() );
		return suite;
	}
}
