/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.subquery;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractSubqueryInSelectClauseTest {

	@BeforeEach
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Person p1 = new Person();
			Person p2 = new Person();
			Document d = new Document();

			p1.getLocalized().put( 1, "p1.1" );
			p1.getLocalized().put( 2, "p1.2" );
			p2.getLocalized().put( 1, "p2.1" );
			p2.getLocalized().put( 2, "p2.2" );

			d.getContacts().put( 1, p1 );
			d.getContacts().put( 2, p2 );

			entityManager.persist( p1 );
			entityManager.persist( p2 );
			entityManager.persist( d );
		} );
	}

	@AfterEach
	public void cleanupTestData(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Document")
	public static class Document {

		private Integer id;

		private Map<Integer, Person> contacts = new HashMap<>();

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
