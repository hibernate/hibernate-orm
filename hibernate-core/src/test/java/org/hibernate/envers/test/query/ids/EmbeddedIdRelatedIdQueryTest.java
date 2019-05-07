/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query.ids;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
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
@Disabled("Revision 3 in prepareAuditData fails - SqmEntityValuedSimplePath#resolvePathPart throws AssertionError")
public class EmbeddedIdRelatedIdQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, Document.class, PersonDocument.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					final Person person = new Person( 1, "Chris" );
					final Document document = new Document( 1, "DL" );
					final PersonDocument pd = new PersonDocument( person, document );
					entityManager.persist( person );
					entityManager.persist( document );
					entityManager.persist( pd );
				},

				// Revision 2
				entityManager -> {
					final Person person = entityManager.find( Person.class, 1 );
					final Document document = new Document( 2, "Passport" );
					final PersonDocument pd = new PersonDocument( person, document );
					entityManager.persist( document );
					entityManager.persist( pd );
				},

				// Revision 3
				entityManager -> {
					final Person person = entityManager.find( Person.class, 1 );
					final Document document = entityManager.find( Document.class, 1 );
					final PersonDocument pd = entityManager
							.createQuery( "FROM PersonDocument WHERE id.person.id = :pid AND id.document.id = :did", PersonDocument.class )
							.setParameter( "pid", person.getId() )
							.setParameter( "did", document.getId() )
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
	public void testRelatedIdQueriesRevision1() {
		final List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
				.add( AuditEntity.relatedId( "id.person" ).eq( 1 ) )
				.add( AuditEntity.revisionNumber().eq( 1 ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 1 ) );

		final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[0] ).getId().getDocument();
		assertThat( document.getName(), equalTo( "DL" ) );
	}

	@DynamicTest
	public void testRelatedIdQueriesRevision2() {
		final List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
				.add( AuditEntity.relatedId( "id.person" ).eq( 1 ) )
				.add( AuditEntity.revisionNumber().eq( 2 ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 1 ) );

		final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[0] ).getId().getDocument();
		assertThat( document.getName(), equalTo( "Passport" ) );
	}

	@DynamicTest
	public void testRelatedIdQueriesRevision3() {
		final List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
				.add( AuditEntity.relatedId( "id.person" ).eq( 1 ) )
				.add( AuditEntity.revisionNumber().eq( 3 ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 1 ) );

		final Document document = ( (PersonDocument) ( (Object[]) results.get( 0 ) )[0] ).getId().getDocument();
		assertThat( document.getName(), nullValue() );
	}

	@DynamicTest
	public void testRelatedIdQueriesDocument1() {
		final List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
				.add( AuditEntity.relatedId( "id.document" ).eq( 1 ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 2 ) );

		for ( Object result : results ) {
			Object[] row = (Object[]) result;
			final RevisionType revisionType = (RevisionType) row[ 2 ];
			final Document document = ( (PersonDocument) row[ 0 ] ).getId().getDocument();
			if ( RevisionType.ADD.equals( revisionType ) ) {
				assertThat( document.getName(), equalTo( "DL" ) );
			}
			else if ( RevisionType.DEL.equals( revisionType ) ) {
				assertThat( document.getName(), nullValue() );
			}
		}
	}

	@DynamicTest
	public void testRelatedIdQueriesDocument2() {
		final List results = getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
				.add( AuditEntity.relatedId( "id.document" ).eq( 2 ) )
				.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 1 ) );

		for ( Object result : results ) {
			Object[] row = (Object[]) result;
			final RevisionType revisionType = (RevisionType) row[2];
			final Document document = ( (PersonDocument) row[0] ).getId().getDocument();
			assertThat( revisionType, equalTo( RevisionType.ADD ) );
			assertThat( document.getName(), equalTo( "Passport" ) );
		}
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
