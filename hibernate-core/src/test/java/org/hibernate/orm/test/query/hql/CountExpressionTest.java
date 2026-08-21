/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				CountExpressionTest.Document.class,
				CountExpressionTest.Person.class,
				CountExpressionTest.CountDistinctTestEntity.class
		}
)
@SessionFactory
public class CountExpressionTest {

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction( session -> {
			Document document = new Document();
			document.setId( 1 );

			Person p1 = new Person();
			Person p2 = new Person();

			p1.getLocalized().put( 1, "p1.1" );
			p1.getLocalized().put( 2, "p1.2" );
			p2.getLocalized().put( 1, "p2.1" );
			p2.getLocalized().put( 2, "p2.2" );

			document.getContacts().put( 1, p1 );
			document.getContacts().put( 2, p2 );

			session.persist( p1 );
			session.persist( p2 );
			session.persist( document );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}


	@Test
	@JiraKey(value = "HHH-9182")
	public void testCountDistinctExpression(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List results = session.createQuery(
							"SELECT " +
							"	d.id, " +
							"	COUNT(DISTINCT CONCAT(CAST(KEY(l) AS String), 'test')) " +
							"FROM Document d " +
							"LEFT JOIN d.contacts c " +
							"LEFT JOIN c.localized l " +
							"GROUP BY d.id" )
					.getResultList();

			assertThat( results ).hasSize( 1 );

			Object[] tuple = (Object[]) results.get( 0 );
			assertThat( tuple[0] ).isEqualTo( 1 );
			assertThat( tuple[1] ).isEqualTo( 2L );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11042")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix allows only one column in count(distinct)")
	public void testCountDistinctTuple(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			List results = session.createQuery(
							"SELECT " +
							"	d.id, " +
							"	COUNT(DISTINCT (KEY(l), l)) " +
							"FROM Document d " +
							"LEFT JOIN d.contacts c " +
							"LEFT JOIN c.localized l " +
							"GROUP BY d.id" )
					.getResultList();

			assertThat( results ).hasSize( 1 );

			Object[] tuple = (Object[]) results.get( 0 );
			assertThat( tuple[0] ).isEqualTo( 1 );
			assertThat( tuple[1] ).isEqualTo( 4L );
		} );
	}

	@Test
	@JiraKey(value = "HHH-11042")
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix allows only one column in count(distinct)")
	public void testCountDistinctTupleSanity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// A simple concatenation of tuple arguments would produce a distinct count of 1 in this case
			// This test checks if the chr(0) count tuple distinct emulation works correctly
			session.persist( new CountDistinctTestEntity( "10", "1" ) );
			session.persist( new CountDistinctTestEntity( "1", "01" ) );
			List<Long> results = session.createQuery( "SELECT count(distinct (t.x,t.y)) FROM CountDistinctTestEntity t",
							Long.class )
					.getResultList();

			assertThat( results ).hasSize( 1 );
			assertThat( results.get( 0 ) ).isEqualTo( 2L );
		} );
	}


	@Entity(name = "Document")
	public static class Document {

		private Integer id;
		private Map<Integer, Person> contacts = new HashMap<>();

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@OneToMany
		@CollectionTable
		@MapKeyColumn(name = "position")
		public Map<Integer, Person> getContacts() {
			return contacts;
		}

		public void setContacts(Map<Integer, Person> contacts) {
			this.contacts = contacts;
		}
	}


	@Entity(name = "Person")
	public static class Person {

		private Integer id;

		private Map<Integer, String> localized = new HashMap<>();

		@Id
		@GeneratedValue
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@ElementCollection
		public Map<Integer, String> getLocalized() {
			return localized;
		}

		public void setLocalized(Map<Integer, String> localized) {
			this.localized = localized;
		}
	}

	@Entity(name = "CountDistinctTestEntity")
	public static class CountDistinctTestEntity {

		private String x;
		private String y;

		public CountDistinctTestEntity() {
		}

		public CountDistinctTestEntity(String x, String y) {
			this.x = x;
			this.y = y;
		}

		@Id
		public String getX() {
			return x;
		}

		public void setX(String x) {
			this.x = x;
		}

		@Id
		public String getY() {
			return y;
		}

		public void setY(String y) {
			this.y = y;
		}
	}

}
