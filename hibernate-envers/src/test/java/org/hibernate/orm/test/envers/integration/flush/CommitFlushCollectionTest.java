/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Version;

import org.hibernate.envers.AuditMappedBy;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.junit.Test;

/**
 * @author Chris Cranford
 */
@JiraKeyGroup( value = {
		@JiraKey(value = "HHH-12826"),
		@JiraKey(value = "HHH-12846")
} )
public class CommitFlushCollectionTest extends BaseEnversJPAFunctionalTestCase {

	@MappedSuperclass
	public static abstract class AbstractEntity {
		private Long id;
		private Long version;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Version
		@Column(nullable = false)
		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}
	}

	@Audited
	@MappedSuperclass
	public static class BaseDocument extends AbstractEntity {
		private String number;
		private Date date;

		@Column(name = "numberValue", nullable = false)
		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		@Column(name = "dateValue", nullable = false)
		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

	@Audited
	@Entity(name = "DocumentA")
	public static class DocumentA extends BaseDocument {
		private List<DocumentLineA> lines = new LinkedList<>();

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
		@AuditMappedBy(mappedBy = "document")
		public List<DocumentLineA> getLines() {
			return lines;
		}

		public void setLines(List<DocumentLineA> lines) {
			this.lines = lines;
		}

		public DocumentA addLine(DocumentLineA line) {
			if ( line != null ) {
				line.setDocument( this );
				getLines().add( line );
			}
			return this;
		}
	}

	@MappedSuperclass
	public abstract static class BaseDocumentLine extends AbstractEntity {
		private String text;

		@Column(name = "textValue", nullable = false)
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Audited
	@Entity(name = "DocumentLineA")
	public static class DocumentLineA extends BaseDocumentLine {
		private DocumentA document;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		@JoinColumn(updatable = false, insertable = true, nullable = false)
		public DocumentA getDocument() {
			return document;
		}

		public void setDocument(DocumentA document) {
			this.document = document;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DocumentA.class, DocumentLineA.class };
	}

	private Long persistDocument(FlushModeType flushModeType) {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.setFlushMode( flushModeType );

			entityManager.getTransaction().begin();
			DocumentA doc = new DocumentA();
			doc.setNumber( "1" );
			doc.setDate( new Date() );

			DocumentLineA line = new DocumentLineA();
			line.setText( "line1" );
			doc.addLine( line );

			entityManager.persist( doc );
			entityManager.getTransaction().commit();

			return doc.getId();
		}
		catch ( Exception e ) {
			if ( entityManager != null && entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			if ( entityManager != null && entityManager.isOpen() ) {
				entityManager.close();
			}
		}
	}

	private void mergeDocument(FlushModeType flushModeType, Long id) {
		final EntityManager entityManager = getOrCreateEntityManager();
		try {
			entityManager.setFlushMode( flushModeType );

			entityManager.getTransaction().begin();
			DocumentA doc = entityManager.find( DocumentA.class, id );
			doc.setDate( new Date() );
			for ( DocumentLineA line : doc.getLines() ) {
				line.setText( "Updated" );
			}

			DocumentLineA line = new DocumentLineA();
			line.setText( "line2" );
			doc.addLine( line );

			entityManager.merge( doc );
			entityManager.getTransaction().commit();
		}
		catch ( Exception e ) {
			if ( entityManager != null && entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			if ( entityManager != null && entityManager.isOpen() ) {
				entityManager.close();
			}
		}
	}

	private Long entityId1;
	private Long entityId2;

	@Test
	@Priority(10)
	public void initData() {
		// This failed when using Envers.
		entityId1 = persistDocument( FlushModeType.COMMIT );

		// This worked
		entityId2 = persistDocument( FlushModeType.AUTO );

		// This failed
		mergeDocument( FlushModeType.COMMIT, entityId1 );

		// This worked
		mergeDocument( FlushModeType.AUTO, entityId2 );
	}

	@Test
	public void testWithFlushModeCommit() {
		assertEquals( Arrays.asList( 1, 3 ), getAuditReader().getRevisions( DocumentA.class, entityId1 ) );
	}

	@Test
	@Priority(1)
	public void testWithFlushModeAuto() {
		assertEquals( Arrays.asList( 2, 4 ), getAuditReader().getRevisions( DocumentA.class, entityId2 ) );
	}
}
