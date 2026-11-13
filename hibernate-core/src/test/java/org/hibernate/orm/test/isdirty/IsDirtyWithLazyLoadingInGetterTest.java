package org.hibernate.orm.test.isdirty;

import java.util.Objects;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that Session.isDirty() doesn't throw ConcurrentModificationException
 * when a mapped getter triggers lazy loading that cascades to eager associations.
 */
@JiraKey("HHH-21061")
public class IsDirtyWithLazyLoadingInGetterTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Document.class, ReferenceData.class, Details.class };
	}

	@BeforeEach
	public void setUp() {
		inTransaction( session -> {
			Details details = new Details();
			details.setId( 1L );
			details.setInfo( "Detail info" );
			session.persist( details );

			ReferenceData reference = new ReferenceData();
			reference.setId( 1L );
			reference.setText( "matching text" );
			reference.setDetails( details );
			session.persist( reference );

			Document document = new Document();
			document.setId( 1L );
			document.setText( "matching text" );
			document.setReferenceData( reference );
			session.persist( document );
		} );
	}

	@Test
	public void testIsDirtyWithLazyLoadingFromGetter() {
		inTransaction( session -> {
			Document document = session.find( Document.class, 1L );

			assertFalse(
					Hibernate.isInitialized( document.getReferenceData() ),
					"ReferenceData should not be initialized yet"
			);

			assertDoesNotThrow( () -> session.isDirty() );

			boolean dirty = session.isDirty();
			assertFalse( dirty, "Session should not be dirty" );

			assertTrue(
					Hibernate.isInitialized( document.getReferenceData() ),
					"ReferenceData should now be initialized"
			);
		} );
	}

	@Test
	public void testIsDirtyDetectsChangeAfterLazyLoading() {
		inTransaction( session -> {
			Document document = session.find( Document.class, 1L );

			assertFalse( session.isDirty(), "Session should not be dirty initially" );

			// Modify the reference data's text too, so validation doesn't fail
			document.getReferenceData().setText( "modified text" );
			document.setText( "modified text" );

			assertTrue( session.isDirty(), "Session should be dirty after modification" );
		} );
	}

	@Test
	public void testIsDirtyWithMultipleLazyLoads() {
		inTransaction( session -> {
			Details details2 = new Details();
			details2.setId( 2L );
			details2.setInfo( "Second detail" );
			session.persist( details2 );

			ReferenceData reference2 = new ReferenceData();
			reference2.setId( 2L );
			reference2.setText( "second text" );
			reference2.setDetails( details2 );
			session.persist( reference2 );

			Document document2 = new Document();
			document2.setId( 2L );
			document2.setText( "second text" );
			document2.setReferenceData( reference2 );
			session.persist( document2 );

			session.flush();
			session.clear();

			Document doc1 = session.find( Document.class, 1L );
			Document doc2 = session.find( Document.class, 2L );

			assertFalse( Hibernate.isInitialized( doc1.getReferenceData() ) );
			assertFalse( Hibernate.isInitialized( doc2.getReferenceData() ) );

			assertDoesNotThrow( () -> session.isDirty() );
			assertFalse( session.isDirty(), "Session should not be dirty" );

			assertTrue( Hibernate.isInitialized( doc1.getReferenceData() ) );
			assertTrue( Hibernate.isInitialized( doc2.getReferenceData() ) );
		} );
	}

	@AfterEach
	public void cleanUp() {
		inTransaction( session -> {
			session.createMutationQuery( "delete from Document" ).executeUpdate();
			session.createMutationQuery( "delete from ReferenceData" ).executeUpdate();
			session.createMutationQuery( "delete from Details" ).executeUpdate();
		} );
	}

	@Entity(name = "Document")
	@Table(name = "document_record")
	@Access(AccessType.PROPERTY)
	public static class Document {
		private Long id;
		private String text;
		private ReferenceData referenceData;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column(name = "text_value")
		public String getText() {
			if ( referenceData != null && !Objects.equals( text, referenceData.getText() ) ) {
				throw new IllegalStateException( "Document and reference text must match" );
			}
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		public ReferenceData getReferenceData() {
			return referenceData;
		}

		public void setReferenceData(ReferenceData referenceData) {
			this.referenceData = referenceData;
		}
	}

	@Entity(name = "ReferenceData")
	@Table(name = "reference_data")
	public static class ReferenceData {
		private Long id;
		private String text;
		private Details details;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column(name = "text_value")
		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@ManyToOne(fetch = FetchType.EAGER)
		public Details getDetails() {
			return details;
		}

		public void setDetails(Details details) {
			this.details = details;
		}
	}

	@Entity(name = "Details")
	@Table(name = "reference_details")
	public static class Details {
		private Long id;
		private String info;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getInfo() {
			return info;
		}

		public void setInfo(String info) {
			this.info = info;
		}
	}
}
