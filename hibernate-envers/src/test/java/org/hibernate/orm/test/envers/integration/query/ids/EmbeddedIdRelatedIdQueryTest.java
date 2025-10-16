/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.query.ids;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Chris Cranford
 */
@Jpa(annotatedClasses = {
		EmbeddedIdRelatedIdQueryTest.Person.class,
		EmbeddedIdRelatedIdQueryTest.Document.class,
		EmbeddedIdRelatedIdQueryTest.PersonDocument.class
})
@EnversTest
@JiraKey(value = "HHH-11748")
public class EmbeddedIdRelatedIdQueryTest {
	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Person person = new Person( 1, "Chris" );
			final Document document = new Document( 1, "DL" );
			final PersonDocument pd = new PersonDocument( person, document );
			entityManager.persist( person );
			entityManager.persist( document );
			entityManager.persist( pd );
		} );

		scope.inTransaction( entityManager -> {
			final Person person = entityManager.find( Person.class, 1 );
			final Document document = new Document( 2, "Passport" );
			final PersonDocument pd = new PersonDocument( person, document );
			entityManager.persist( document );
			entityManager.persist( pd );
		} );

		scope.inTransaction( entityManager -> {
			final Person person = entityManager.find( Person.class, 1 );
			final Document document = entityManager.find( Document.class, 1 );
			final PersonDocument pd = entityManager
					.createQuery( "FROM PersonDocument WHERE id.person.id = :person AND id.document.id = :document",
							PersonDocument.class )
					.setParameter( "person", person.getId() )
					.setParameter( "document", document.getId() )
					.getSingleResult();

			entityManager.remove( pd );
			entityManager.remove( document );
		} );
	}

	@Test
	public void testRevisionCounts(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1 ), AuditReaderFactory.get( em ).getRevisions( Person.class, 1 ) );
			assertEquals( Arrays.asList( 1, 3 ), AuditReaderFactory.get( em ).getRevisions( Document.class, 1 ) );
			assertEquals( Arrays.asList( 2 ), AuditReaderFactory.get( em ).getRevisions( Document.class, 2 ) );
		} );
	}

	@Test
	public void testRelatedIdQueries(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "id.person" ).eq( 1 ) )
					.add( AuditEntity.revisionNumber().eq( 1 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			final Document document = ((PersonDocument) ((Object[]) results.get( 0 ))[0]).getId().getDocument();
			assertEquals( "DL", document.getName() );
		} );

		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "id.person" ).eq( 1 ) )
					.add( AuditEntity.revisionNumber().eq( 2 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			final Document document = ((PersonDocument) ((Object[]) results.get( 0 ))[0]).getId().getDocument();
			assertEquals( "Passport", document.getName() );
		} );

		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "id.person" ).eq( 1 ) )
					.add( AuditEntity.revisionNumber().eq( 3 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			final Document document = ((PersonDocument) ((Object[]) results.get( 0 ))[0]).getId().getDocument();
			assertNull( document.getName() );
		} );

		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "id.document" ).eq( 1 ) )
					.getResultList();
			assertEquals( 2, results.size() );
			for ( Object result : results ) {
				Object[] row = (Object[]) result;
				final RevisionType revisionType = (RevisionType) row[2];
				final Document document = ((PersonDocument) row[0]).getId().getDocument();
				if ( RevisionType.ADD.equals( revisionType ) ) {
					assertEquals( "DL", document.getName() );
				}
				else if ( RevisionType.DEL.equals( revisionType ) ) {
					assertNull( document.getName() );
				}
			}
		} );

		scope.inEntityManager( em -> {
			List results = AuditReaderFactory.get( em ).createQuery()
					.forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "id.document" ).eq( 2 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			for ( Object result : results ) {
				Object[] row = (Object[]) result;
				final RevisionType revisionType = (RevisionType) row[2];
				final Document document = ((PersonDocument) row[0]).getId().getDocument();
				assertEquals( RevisionType.ADD, revisionType );
				assertEquals( "Passport", document.getName() );
			}
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

	@Audited
	@Entity(name = "PersonDocument")
	public static class PersonDocument implements Serializable {
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

	@Audited
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
	@Audited
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
