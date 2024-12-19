/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytoone.bidirectional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.Audited;
import org.hibernate.envers.EntityTrackingRevisionListener;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionListener;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import org.hibernate.envers.RevisionType;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Jira( "https://hibernate.atlassian.net/browse/HHH-17652" )
public class ManyToOneCustomRevisionListenerTest extends BaseEnversJPAFunctionalTestCase {
	private static final ThreadLocal<AuditReader> auditReader = ThreadLocal.withInitial( () -> null );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Document.class,
				DocumentAuthorEmployee.class,
				Employee.class,
				CustomRevisionEntity.class,
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		// store in thread-local to use it in custom revision listener
		auditReader.set( getAuditReader() );

		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		final Employee bilbo = new Employee( "Bilbo Baggins" );
		em.persist( bilbo );
		final Employee frodo = new Employee( "Frodo Baggins" );
		em.persist( frodo );

		em.getTransaction().commit();

		em.getTransaction().begin();

		final Document document = new Document( "The Hobbit" );
		document.getAuthors().add( new DocumentAuthorEmployee( 1L, document, bilbo ) );
		document.getAuthors().add( new DocumentAuthorEmployee( 2L, document, frodo ) );
		em.persist( document );

		em.getTransaction().commit();
	}

	@Test
	public void testDocumentAuthorEmployeeRevisions() {
		final AuditReader reader = getAuditReader();
		assertLastRevision( reader, 1L, "Bilbo Baggins" );
		assertLastRevision( reader, 2L, "Frodo Baggins" );
		getEntityManager().close();
	}

	private static void assertLastRevision(AuditReader reader, Long id, String employee) {
		final List<Number> revisions = reader.getRevisions( DocumentAuthorEmployee.class, id );
		final Number revisionNumber = revisions.get( revisions.size() - 1 );
		final DocumentAuthorEmployee result = reader.find( DocumentAuthorEmployee.class, id, revisionNumber );
		assertThat( result.getEmployee().getName() ).isEqualTo( employee );
		assertThat( result.getDocument().getTitle() ).isEqualTo( "The Hobbit" );
	}

	@Audited(withModifiedFlag = true)
	@Entity(name = "Document")
	static class Document {
		@Id
		@GeneratedValue
		private Long id;

		private String title;

		@OneToMany(mappedBy = "document", cascade = CascadeType.ALL)
		private List<DocumentAuthorEmployee> authors = new ArrayList<>();

		public Document() {
		}

		public Document(String title) {
			this.title = title;
		}

		public List<DocumentAuthorEmployee> getAuthors() {
			return authors;
		}

		public String getTitle() {
			return title;
		}
	}

	@Audited(withModifiedFlag = true)
	@Entity(name = "DocumentAuthorEmployee")
	static class DocumentAuthorEmployee {
		@Id
		private Long id;

		@ManyToOne
		@JoinColumn(name = "document_id")
		private Document document;

		@ManyToOne(fetch = FetchType.EAGER)
		@JoinColumn(name = "employee_id")
		private Employee employee;

		public DocumentAuthorEmployee() {
		}

		public DocumentAuthorEmployee(Long id, Document document, Employee employee) {
			this.id = id;
			this.document = document;
			this.employee = employee;
		}

		public Long getId() {
			return id;
		}

		public Document getDocument() {
			return document;
		}

		public Employee getEmployee() {
			return employee;
		}
	}

	@Audited(withModifiedFlag = true)
	@Entity(name = "Employee")
	static class Employee {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Employee() {
		}

		public Employee(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "CustomRevisionEntity")
	@RevisionEntity(CustomRevisionListener.class)
	static class CustomRevisionEntity {
		@Id
		@GeneratedValue
		@RevisionNumber
		private int id;

		@RevisionTimestamp
		private long timestamp;
	}

	static class CustomRevisionListener implements RevisionListener, EntityTrackingRevisionListener {
		@SuppressWarnings({"unchecked", "rawtypes"})
		@Override
		public void entityChanged(Class entityClass, String entityName, Object entityId, RevisionType revisionType, Object revisionEntity) {
			final AuditReader reader = auditReader.get();
			final List<Number> revisions = reader.getRevisions( entityClass, entityId );
			final Number revisionNumber = revisions.get( revisions.size() - 1 );

			// This is what triggered the NPE
			final Object obj = reader.find( entityClass, entityId, revisionNumber );
			assertThat( obj ).isNotNull();
		}

		@Override
		public void newRevision(Object revisionEntity) {
		}
	}
}
