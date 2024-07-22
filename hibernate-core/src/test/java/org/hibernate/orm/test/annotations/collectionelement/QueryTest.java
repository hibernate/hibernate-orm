/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = { EntityWithAnElementCollection.class }
)
@SessionFactory
public class QueryTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityWithAnElementCollection entityWithAnElementCollection = new EntityWithAnElementCollection();
					entityWithAnElementCollection.setId( 1L );
					entityWithAnElementCollection.addSomeString( "abc" );
					entityWithAnElementCollection.addSomeString( "efg" );

					session.persist( entityWithAnElementCollection );

					EntityWithAnElementCollection entityWithAnElementCollection2 = new EntityWithAnElementCollection();
					entityWithAnElementCollection2.setId( 2L );
					entityWithAnElementCollection2.addSomeString( "hil" );
					entityWithAnElementCollection2.addSomeString( "mnp" );

					session.persist( entityWithAnElementCollection2 );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5209")
	public void testMemberOfSyntax(SessionFactoryScope scope) {
		// performs syntax checking of the MEMBER OF predicate against a basic collection
		scope.inSession(
				session -> {
					List<EntityWithAnElementCollection> list = session.createQuery(
							"from EntityWithAnElementCollection e where 'abc' member of e.someStrings" )
							.list();
					assertThat( list.size(), is( 1 ) );
				}
		);
	}
}
