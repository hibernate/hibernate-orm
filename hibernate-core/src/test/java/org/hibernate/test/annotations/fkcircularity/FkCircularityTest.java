// $Id$
package org.hibernate.test.annotations.fkcircularity;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import org.jboss.logging.Logger;

/**
 * Test case for ANN-722 and ANN-730.
 *
 * @author Hardy Ferentschik
 */
public class FkCircularityTest extends BaseUnitTestCase {
	private static final Logger log = Logger.getLogger( FkCircularityTest.class );


    @Test
	public void testJoinedSublcassesInPK() {
		new MetadataSources()
				.addAnnotatedClass(A.class)
				.addAnnotatedClass(B.class)
				.addAnnotatedClass(C.class)
				.addAnnotatedClass(D.class)
				.buildMetadata();
	}

    @Test
	public void testDeepJoinedSuclassesHierachy() {
		new MetadataSources()
				.addAnnotatedClass(ClassA.class)
				.addAnnotatedClass(ClassB.class)
				.addAnnotatedClass(ClassC.class)
				.addAnnotatedClass(ClassD.class)
				.buildMetadata();
	}
}
