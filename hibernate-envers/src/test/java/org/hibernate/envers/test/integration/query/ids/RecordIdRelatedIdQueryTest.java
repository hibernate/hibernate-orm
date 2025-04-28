/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.test.integration.query.ids;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.processing.Exclude;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.Test;

import org.hibernate.testing.transaction.TransactionUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@Exclude
@Jira("https://hibernate.atlassian.net/browse/HHH-19393")
public class RecordIdRelatedIdQueryTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Document.class, PersonDocument.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Person person = new Person( 1, "Chris" );
			final Document document = new Document( 1, "DL" );
			final PersonDocument pd = new PersonDocument( person, document );
			entityManager.persist( person );
			entityManager.persist( document );
			entityManager.persist( pd );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Person person = entityManager.find( Person.class, 1 );
			final Document document = new Document( 2, "Passport" );
			final PersonDocument pd = new PersonDocument( person, document );
			entityManager.persist( document );
			entityManager.persist( pd );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			final Person person = entityManager.find( Person.class, 1 );
			final Document document = entityManager.find( Document.class, 1 );
			final PersonDocument pd = entityManager
					.createQuery( "FROM PersonDocument WHERE person.id = :person AND document.id = :document", PersonDocument.class )
					.setParameter( "person", person.getId() )
					.setParameter( "document", document.getId() )
					.getSingleResult();

			entityManager.remove( pd );
			entityManager.remove( document );
		} );
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Person.class, 1 ) );
		assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( Document.class, 1 ) );
		assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( Document.class, 2 ) );
	}

	@Test
	public void testRelatedIdQueries() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "person" ).eq( 1 ) )
					.add( AuditEntity.revisionNumber().eq( 1 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[0] ).getDocument();
			assertEquals( "DL", document.getName() );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "person" ).eq( 1 ) )
					.add( AuditEntity.revisionNumber().eq( 2 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[0] ).getDocument();
			assertEquals( "Passport", document.getName() );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "person" ).eq( 1 ) )
					.add( AuditEntity.revisionNumber().eq( 3 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[0] ).getDocument();
			assertNull( document.getName() );
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "document" ).eq( 1 ) )
					.getResultList();
			assertEquals( 2, results.size() );
			for ( Object result : results ) {
				Object[] row = (Object[]) result;
				final RevisionType revisionType = (RevisionType) row[2];
				final Document document = ( (PersonDocument) row[0] ).getDocument();
				if ( RevisionType.ADD.equals( revisionType ) ) {
					assertEquals( "DL", document.getName() );
				}
				else if ( RevisionType.DEL.equals( revisionType ) ) {
					assertNull( document.getName() );
				}
			}
		} );

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
					.add( AuditEntity.relatedId( "document" ).eq( 2 ) )
					.getResultList();
			assertEquals( 1, results.size() );
			for ( Object result : results ) {
				Object[] row = (Object[]) result;
				final RevisionType revisionType = (RevisionType) row[2];
				final Document document = ( (PersonDocument) row[0] ).getDocument();
				assertEquals( RevisionType.ADD, revisionType );
				assertEquals( "Passport", document.getName() );
			}
		} );
	}

	@Audited
	@Entity(name = "PersonDocument")
	@IdClass( PersonDocumentId.class )
	public static class PersonDocument implements Serializable {
		@Id
		@ManyToOne(optional = false)
		private Document document;

		@Id
		@ManyToOne(optional = false)
		private Person person;

		PersonDocument() {

		}

		PersonDocument(Person person, Document document) {
			this.document = document;
			this.person = person;
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

	public record PersonDocumentId(Document document, Person person) {
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
