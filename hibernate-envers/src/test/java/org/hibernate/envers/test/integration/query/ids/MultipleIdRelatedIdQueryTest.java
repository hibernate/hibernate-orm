/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query.ids;

import java.io.Serializable;
import java.util.Arrays;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11748")
public class MultipleIdRelatedIdQueryTest extends BaseEnversJPAFunctionalTestCase {
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
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Person.class, 1 ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( Document.class, 1 ) );
	}

	@Test
	public void testQuery() {
		getAuditReader().createQuery().forRevisionsOfEntity( PersonDocument.class, false, true )
				.add( AuditEntity.relatedId( "person" ).eq( 1 ) )
				.getResultList();
	}

	@Audited
	@Entity(name = "PersonDocument")
	public static class PersonDocument implements Serializable {
		@Id
		@ManyToOne(optional = false)
		private Document document;

		@Id
		@OneToOne(optional = false)
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
