/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { EntityWithAnElementCollection.class }
)
@SessionFactory
public class QueryTest {

	@Test
	@TestForIssue(jiraKey = "HHH-5209")
	public void testMemberOfSyntax(SessionFactoryScope scope) {
		// performs syntax checking of the MEMBER OF predicate against a basic collection
		scope.inSession(
				session ->
						session.createQuery( "from EntityWithAnElementCollection e where 'abc' member of e.someStrings" )
								.list()
		);
	}
}
