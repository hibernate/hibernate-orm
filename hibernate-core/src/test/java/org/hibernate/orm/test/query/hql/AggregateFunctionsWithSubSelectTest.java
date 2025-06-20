/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author bjoern.moritz
 */
@JiraKey(value = "HHH-9331")
@RequiresDialect(H2Dialect.class)
@DomainModel(
		annotatedClasses = {
				AggregateFunctionsWithSubSelectTest.Document.class,
				AggregateFunctionsWithSubSelectTest.Person.class
		}
)
@SessionFactory
public class AggregateFunctionsWithSubSelectTest {
	@BeforeEach
	protected void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					Document document = new Document();
					document.setId( 1 );

					Person p1 = new Person();
					Person p2 = new Person();

					p1.getLocalized().put(1, "p1.1");
					p1.getLocalized().put(2, "p1.2");
					p2.getLocalized().put(1, "p2.1");
					p2.getLocalized().put(2, "p2.2");

					document.getContacts().put(1, p1);
					document.getContacts().put(2, p2);

					session.persist(p1);
					session.persist(p2);
					session.persist(document);
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSum(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					List results = session.createQuery(
							"SELECT " +
									"	d.id, " +
									"	SUM(" +
									"		(" +
									"			SELECT COUNT(localized) " +
									"			FROM Person p " +
									"			LEFT JOIN p.localized localized " +
									"			WHERE p.id = c.id" +
									"		)" +
									"	) AS localizedCount " +
									"FROM Document d " +
									"LEFT JOIN d.contacts c " +
									"GROUP BY d.id")
							.getResultList();

					assertEquals(1, results.size());
					Object[] tuple = (Object[]) results.get( 0 );
					assertEquals(1, tuple[0]);
				}
		);
	}

	@Test
	public void testMin(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					List results = session.createQuery(
							"SELECT " +
									"	d.id, " +
									"	MIN(" +
									"		(" +
									"			SELECT COUNT(localized) " +
									"			FROM Person p " +
									"			LEFT JOIN p.localized localized " +
									"			WHERE p.id = c.id" +
									"		)" +
									"	) AS localizedCount " +
									"FROM Document d " +
									"LEFT JOIN d.contacts c " +
									"GROUP BY d.id")
							.getResultList();

					assertEquals(1, results.size());
					Object[] tuple = (Object[]) results.get( 0 );
					assertEquals(1, tuple[0]);
				}
		);
	}

	@Test
	public void testMax(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry = "SELECT " +
							"	d.id, " +
							"	MAX(" +
							"		(" +
							"			SELECT COUNT(localized) " +
							"			FROM Person p " +
							"			LEFT JOIN p.localized localized " +
							"			WHERE p.id = c.id" +
							"		)" +
							"	) AS localizedCount " +
							"FROM Document d " +
							"LEFT JOIN d.contacts c " +
							"GROUP BY d.id";
					List results = session.createQuery( qry ).getResultList();

					assertEquals(1, results.size());
					Object[] tuple = (Object[]) results.get( 0 );
					assertEquals(1, tuple[0]);
				}
		);
	}

	@Test
	public void testAvg(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qry = "SELECT " +
							"	d.id, " +
							"	AVG(" +
							"		(" +
							"			SELECT COUNT(localized) " +
							"			FROM Person p " +
							"			LEFT JOIN p.localized localized " +
							"			WHERE p.id = c.id" +
							"		)" +
							"	) AS localizedCount " +
							"FROM Document d " +
							"LEFT JOIN d.contacts c " +
							"GROUP BY d.id";
					List results = session.createQuery( qry ).getResultList();

					assertEquals(1, results.size());
					Object[] tuple = (Object[]) results.get( 0 );
					assertEquals(1, tuple[0]);
				}
		);
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

}
