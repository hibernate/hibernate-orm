/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query.ids;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11748")
@Disabled("NYI - Multiple @Id support")
public class MultipleIdRelatedIdQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Document.class, PersonDocument.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransaction(
				entityManager -> {
					final Person person = new Person( 1, "Chris" );
					final Document document = new Document( 1, "DL" );
					final PersonDocument pd = new PersonDocument( person, document );
					entityManager.persist( person );
					entityManager.persist( document );
					entityManager.persist( pd );
				}
		);

		inTransaction(
				entityManager -> {
					final Person person = entityManager.find( Person.class, 1 );
					final Document document = new Document( 2, "Passport" );
					final PersonDocument pd = new PersonDocument( person, document );
					entityManager.persist( document );
					entityManager.persist( pd );
				}
		);

		inTransaction(
				entityManager -> {
					final Person person = entityManager.find( Person.class, 1 );
					final Document document = entityManager.find( Document.class, 1 );
					final PersonDocument pd = entityManager
							.createQuery( "FROM PersonDocument WHERE person.id = :person AND document.id = :document", PersonDocument.class )
							.setParameter( "person", person.getId() )
							.setParameter( "document", document.getId() )
							.getSingleResult();

					entityManager.remove( pd );
					entityManager.remove( document );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Person.class, 1 ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( Document.class, 1 ), contains( 1, 3 ) );
		assertThat( getAuditReader().getRevisions( Document.class, 2 ), contains( 2 ) );
	}

	@DynamicTest
	public void testRelatedIdQueries() {
		inTransaction(
				entityManager -> {
					List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
							.add( AuditEntity.relatedId( "person" ).eq( 1 ) )
							.add( AuditEntity.revisionNumber().eq( 1 ) )
							.getResultList();
					assertThat( results, CollectionMatchers.hasSize( 1 ) );
					final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[ 0 ] ).getDocument();
					assertThat( document.getName(), equalTo( "DL" ) );
				}
		);

		inTransaction(
				entityManager -> {
					List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
							.add( AuditEntity.relatedId( "person" ).eq( 1 ) )
							.add( AuditEntity.revisionNumber().eq( 2 ) )
							.getResultList();
					assertThat( results, CollectionMatchers.hasSize( 1 ) );
					final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[ 0 ] ).getDocument();
					assertThat( document.getName(), equalTo( "Passport" ) );
				}
		);

		inTransaction(
				entityManager -> {
					List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
							.add( AuditEntity.relatedId( "person" ).eq( 1 ) )
							.add( AuditEntity.revisionNumber().eq( 3 ) )
							.getResultList();
					assertThat( results, CollectionMatchers.hasSize( 1 ) );
					final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[ 0 ] ).getDocument();
					assertThat( document.getName(), nullValue() );
				}
		);

		inTransaction(
				entityManager -> {
					List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
							.add( AuditEntity.relatedId( "document" ).eq( 1 ) )
							.getResultList();
					assertThat( results, CollectionMatchers.hasSize( 2 ) );
					for ( Object result : results ) {
						Object[] row = (Object[]) result;
						final RevisionType revisionType = (RevisionType) row[ 2 ];
						final Document document = ( (PersonDocument) row[ 0 ] ).getDocument();
						if ( RevisionType.ADD.equals( revisionType ) ) {
							assertThat( document.getName(), equalTo( "DL" ) );
						}
						else if ( RevisionType.DEL.equals( revisionType ) ) {
							assertThat( document.getName(), nullValue() );
						}
					}
				}
		);

		inTransaction(
				entityManager -> {
					List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
							.add( AuditEntity.relatedId( "document" ).eq( 2 ) )
							.getResultList();
					assertThat( results, CollectionMatchers.hasSize( 1 ) );
					for ( Object result : results ) {
						Object[] row = (Object[]) result;
						final RevisionType revisionType = (RevisionType) row[ 2 ];
						final Document document = ( (PersonDocument) row[ 0 ] ).getDocument();
						assertThat( revisionType, equalTo( RevisionType.ADD ) );
						assertThat( document.getName(), equalTo( "Passport" ) );
					}
				}
		);
	}

	@Audited
	@Entity(name = "PersonDocument")
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
