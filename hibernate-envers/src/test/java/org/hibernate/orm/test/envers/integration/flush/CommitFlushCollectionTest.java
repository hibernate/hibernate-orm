/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.flush;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Chris Cranford
 */
@JiraKeyGroup( value = {
		@JiraKey(value = "HHH-12826"),
		@JiraKey(value = "HHH-12846")
} )
@EnversTest
@Jpa(annotatedClasses = {CommitFlushCollectionTest.DocumentA.class, CommitFlushCollectionTest.DocumentLineA.class})
public class CommitFlushCollectionTest {

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

	private Long entityId1;
	private Long entityId2;

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		// This failed when using Envers.
		entityId1 = persistDocument( scope, FlushModeType.COMMIT );

		// This worked
		entityId2 = persistDocument( scope, FlushModeType.AUTO );

		// This failed
		mergeDocument( scope, FlushModeType.COMMIT, entityId1 );

		// This worked
		mergeDocument( scope, FlushModeType.AUTO, entityId2 );
	}

	private Long persistDocument(EntityManagerFactoryScope scope, FlushModeType flushModeType) {
		return scope.fromTransaction( em -> {
			em.setFlushMode( flushModeType );

			DocumentA doc = new DocumentA();
			doc.setNumber( "1" );
			doc.setDate( new Date() );

			DocumentLineA line = new DocumentLineA();
			line.setText( "line1" );
			doc.addLine( line );

			em.persist( doc );
			return doc.getId();
		} );
	}

	private void mergeDocument(EntityManagerFactoryScope scope, FlushModeType flushModeType, Long id) {
		scope.inTransaction( em -> {
			em.setFlushMode( flushModeType );

			DocumentA doc = em.find( DocumentA.class, id );
			doc.setDate( new Date() );
			for ( DocumentLineA line : doc.getLines() ) {
				line.setText( "Updated" );
			}

			DocumentLineA line = new DocumentLineA();
			line.setText( "line2" );
			doc.addLine( line );

			em.merge( doc );
		} );
	}

	@Test
	public void testWithFlushModeCommit(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 1, 3 ),
					AuditReaderFactory.get( em ).getRevisions( DocumentA.class, entityId1 ) );
		} );
	}

	@Test
	public void testWithFlushModeAuto(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			assertEquals( Arrays.asList( 2, 4 ),
					AuditReaderFactory.get( em ).getRevisions( DocumentA.class, entityId2 ) );
		} );
	}
}
