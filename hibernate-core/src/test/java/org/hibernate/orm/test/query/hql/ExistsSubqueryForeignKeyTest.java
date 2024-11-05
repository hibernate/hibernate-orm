/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.HSQLDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		ExistsSubqueryForeignKeyTest.Person.class,
		ExistsSubqueryForeignKeyTest.Document.class,
})
@SessionFactory
@SkipForDialect(dialectClass = HSQLDialect.class, reason = "HSQLDB doesn't like the case-when selection not being in the group-by")
@SkipForDialect(dialectClass = DerbyDialect.class, reason = "Derby doesn't like the case-when selection not being in the group-by")
@Jira( "https://hibernate.atlassian.net/browse/HHH-18816" )
public class ExistsSubqueryForeignKeyTest {
	@Test
	public void testWhereClause(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long result = session.createQuery(
					"select count(*) from Document d join d.owner o "
							+ "where exists(select p.id from Person p where p.id = o.id) group by o.id",
					Long.class
			).getSingleResult();
			assertThat( result ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testSelectCaseWhen(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select case when exists(select p.id from Person p where p.id = o.id) then 1 else 0 end,"
							+ "count(*) from Document d join d.owner o group by o.id",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0, Integer.class ) ).isEqualTo( 1 );
			assertThat( result.get( 1, Long.class ) ).isEqualTo( 1L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person1 = new Person( 1L, "person_1" );
			session.persist( person1 );
			session.persist( new Document( 1L, "doc_1", person1 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "Person")
	static class Person {
		@Id
		private Long id;

		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "Document")
	static class Document {
		@Id
		private Long id;

		private String title;

		@ManyToOne
		private Person owner;

		public Document() {
		}

		public Document(Long id, String title, Person owner) {
			this.id = id;
			this.title = title;
			this.owner = owner;
		}
	}
}
