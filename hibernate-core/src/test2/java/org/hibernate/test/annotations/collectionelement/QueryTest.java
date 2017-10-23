/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement;

import org.hibernate.Session;

import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class QueryTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { EntityWithAnElementCollection.class };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5209" )
	public void testMemberOfSyntax() {
		// performs syntax checking of the MEMBER OF predicate against a basic collection
		Session s = openSession();
		s.createQuery( "from EntityWithAnElementCollection e where 'abc' member of e.someStrings" ).list();
		s.close();
	}
}
