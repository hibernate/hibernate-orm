/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10893")
@DomainModel(annotatedClasses = ParameterCollectionTest.Person.class)
@SessionFactory
public class ParameterCollectionTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			for ( int i = 0; i < 20; i++ ) {
				Person p1 = new Person( i, "p" + i );
				session.persist( p1 );
			}
		} );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testReusingQueryWithNewParameterValues(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			Collection<Long> ids = new ArrayList<>();
			Query q = session.createQuery( "select id from Person where id in (:ids) order by id" );
			for ( int i = 0; i < 10; i++ ) {
				ids.add( Long.valueOf( i ) );
			}
			q.setParameterList( "ids", ids );
			q.list();

			ids.clear();
			for ( int i = 10; i < 20; i++ ) {
				ids.add( Long.valueOf( i ) );
			}
			// reuse the same query, but set new collection parameter
			q.setParameterList( "ids", ids );
			List<Long> foundIds = q.list();

			assertThat( foundIds )
					.withFailMessage( "Wrong number of results" )
					.hasSize( ids.size() );
			assertThat( foundIds ).containsAll( ids );
		} );
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	public static class Person {
		@Id
		private long id;
		private String name;

		public Person() {
		}

		public Person(long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
