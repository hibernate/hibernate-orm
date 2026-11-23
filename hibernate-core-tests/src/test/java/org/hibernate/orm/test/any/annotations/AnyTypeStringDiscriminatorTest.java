package org.hibernate.orm.test.any.annotations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// TYPE() comparisons against string discriminator values of an Any association.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { AnyTypeStringDiscriminatorTest.DocumentRow.class,
		AnyTypeStringDiscriminatorTest.Frame.class, AnyTypeStringDiscriminatorTest.Lens.class } )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-13323" )
class AnyTypeStringDiscriminatorTest {
	@Test
	void compareTypeToStringLiteral(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Frame frame = new Frame();
			frame.id = 1L;
			Lens lens = new Lens();
			lens.id = 1L;
			session.persist( frame );
			session.persist( lens );
			session.persist( new DocumentRow( 1L, frame ) );
			session.persist( new DocumentRow( 2L, lens ) );
			session.persist( new DocumentRow( 3L, null ) );
		} );
		scope.inTransaction( session -> {
			var frames = session.createQuery(
					"from AnyDocumentRow row where type(row.product) = 'F'", DocumentRow.class )
					.getResultList();
			assertThat( frames ).extracting( row -> row.id ).containsExactly( 1L );
			assertThat( frames.get( 0 ).product ).isInstanceOf( Frame.class );

			var lenses = session.createQuery(
					"from AnyDocumentRow row where type(row.product) = 'OL'", DocumentRow.class )
					.getResultList();
			assertThat( lenses ).extracting( row -> row.id ).containsExactly( 2L );
			assertThat( lenses.get( 0 ).product ).isInstanceOf( Lens.class );
		} );
	}

	public interface Product {
	}

	@Entity( name = "AnyDocumentRow" )
	public static class DocumentRow {
		@Id
		Long id;
		@Any( optional = true )
		@AnyKeyJavaClass( Long.class )
		@AnyDiscriminatorValue( discriminator = "F", entity = Frame.class )
		@AnyDiscriminatorValue( discriminator = "OL", entity = Lens.class )
		@Column( name = "product_group" )
		@JoinColumn( name = "product_id" )
		Product product;

		public DocumentRow() {
		}

		DocumentRow(Long id, Product product) {
			this.id = id;
			this.product = product;
		}
	}

	@Entity( name = "AnyFrame" )
	public static class Frame implements Product {
		@Id
		Long id;
	}

	@Entity( name = "AnyLens" )
	public static class Lens implements Product {
		@Id
		Long id;
	}
}
