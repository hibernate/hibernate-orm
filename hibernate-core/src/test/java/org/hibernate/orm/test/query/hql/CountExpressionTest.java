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

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Christian Beikov
 */
public class CountExpressionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			Document.class,
			Person.class,
			CountDistinctTestEntity.class
		};
	}

	@Override
	protected void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
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
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@JiraKey(value = "HHH-9182")
	public void testCountDistinctExpression() {
		doInHibernate( this::sessionFactory, session -> {
			List results = session.createQuery(
				"SELECT " +
				"	d.id, " +
				"	COUNT(DISTINCT CONCAT(CAST(KEY(l) AS String), 'test')) " +
				"FROM Document d " +
				"LEFT JOIN d.contacts c " +
				"LEFT JOIN c.localized l " +
				"GROUP BY d.id")
			.getResultList();

			assertEquals(1, results.size());
			Object[] tuple = (Object[]) results.get( 0 );
			assertEquals(1, tuple[0]);
			assertEquals(2L, tuple[1]);
		} );
	}

	@Test
	@JiraKey(value = "HHH-11042")
	public void testCountDistinctTuple() {
		doInHibernate( this::sessionFactory, session -> {
			List results = session.createQuery(
							"SELECT " +
									"	d.id, " +
									"	COUNT(DISTINCT (KEY(l), l)) " +
									"FROM Document d " +
									"LEFT JOIN d.contacts c " +
									"LEFT JOIN c.localized l " +
									"GROUP BY d.id")
					.getResultList();

			assertEquals(1, results.size());
			Object[] tuple = (Object[]) results.get( 0 );
			assertEquals(1, tuple[0]);
			assertEquals(4L, tuple[1]);
		} );
	}

	@Test
	@JiraKey(value = "HHH-11042")
	public void testCountDistinctTupleSanity() {
		doInHibernate( this::sessionFactory, session -> {
			// A simple concatenation of tuple arguments would produce a distinct count of 1 in this case
			// This test checks if the chr(0) count tuple distinct emulation works correctly
			session.persist( new CountDistinctTestEntity("10", "1") );
			session.persist( new CountDistinctTestEntity("1", "01") );
			List<Long> results = session.createQuery("SELECT count(distinct (t.x,t.y)) FROM CountDistinctTestEntity t", Long.class)
					.getResultList();

			assertEquals(1, results.size());
			assertEquals( 2L, results.get( 0 ).longValue() );
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
