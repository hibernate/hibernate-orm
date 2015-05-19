/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
