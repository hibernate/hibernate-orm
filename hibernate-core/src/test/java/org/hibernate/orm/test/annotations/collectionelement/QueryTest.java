/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.collectionelement;

import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
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
	@JiraKey(value = "HHH-5209")
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
