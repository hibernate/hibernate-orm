/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.junit.Before;

public abstract class AbstractSubqueryInSelectClauseTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Person.class, Document.class };
	}

	@Before
	public void initData() {
		doInJPA( this::entityManagerFactory, em -> {
			Person p1 = new Person();
			Person p2 = new Person();
			Document d = new Document();

			p1.getLocalized().put( 1, "p1.1" );
			p1.getLocalized().put( 2, "p1.2" );
			p2.getLocalized().put( 1, "p2.1" );
			p2.getLocalized().put( 2, "p2.2" );

			d.getContacts().put( 1, p1 );
			d.getContacts().put( 2, p2 );

			em.persist( p1 );
			em.persist( p2 );
			em.persist( d );
		} );
	}

	@Entity(name = "Document")
	public static class Document {

		private Integer id;

		private Map<Integer, Person> contacts = new HashMap<Integer, Person>();

		@Id
		@GeneratedValue
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

		private Map<Integer, String> localized = new HashMap<Integer, String>();

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
