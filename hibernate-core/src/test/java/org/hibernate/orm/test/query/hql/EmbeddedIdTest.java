/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@DomainModel(
		annotatedClasses = {
				EmbeddedIdTest.PersonDocument.class,
				EmbeddedIdTest.Document.class,
				EmbeddedIdTest.Person.class,
		}
)
@SessionFactory
public class EmbeddedIdTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Person person = new Person( 1, "Chris" );
			final Document document = new Document( 1, "DL" );
			final PersonDocument pd = new PersonDocument( person, document );
			entityManager.persist( person );
			entityManager.persist( document );
			entityManager.persist( pd );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testHqlQueryWithAlias(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final PersonDocument pd = entityManager
					.createQuery(
							"FROM PersonDocument p WHERE p.id.person.id = :person",
							PersonDocument.class
					)
					.setParameter( "person", 1 )
					.getSingleResult();
			assertThat( pd, is( notNullValue() ) );
		} );
	}

	@Test
	public void testHqlQueryWithoutAlias(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final PersonDocument pd = entityManager
					.createQuery(
							"FROM PersonDocument WHERE id.person.id = :person",
							PersonDocument.class
					)
					.setParameter( "person", 1 )
					.getSingleResult();
			assertThat( pd, is( notNullValue() ) );
		} );
	}

	@Embeddable
	public static class PersonDocumentId implements Serializable {
		@ManyToOne(optional = false)
		private Document document;

		@ManyToOne(optional = false)
		private Person person;

		PersonDocumentId() {
		}

		PersonDocumentId(Person person, Document document) {
			this.person = person;
			this.document = document;
		}

		public Document getDocument() {
			return document;
		}

		public void setDocument(Document document) {
			this.document = document;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

	@Entity(name = "PersonDocument")
	public static class PersonDocument {
		@EmbeddedId
		private PersonDocumentId id;

		PersonDocument() {
		}

		PersonDocument(Person person, Document document) {
			this.id = new PersonDocumentId( person, document );
		}

		public PersonDocumentId getId() {
			return id;
		}

		public void setId(PersonDocumentId id) {
			this.id = id;
		}
	}

	@Entity(name = "Document")
	public static class Document {
		@Id
		private Integer id;
		private String name;

		Document() {
		}

		Document(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Person")
	public static class Person {
		@Id
		private Integer id;
		private String name;

		Person() {

		}

		Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
