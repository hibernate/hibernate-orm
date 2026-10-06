package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;

import org.hibernate.annotations.CompositeType;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// A Matrix-style value with a generic list array and void setters must bootstrap
/// with callbacks enabled and round-trip through the current CompositeUserType SPI.
/// The projection supplies the persistent representation of the opaque value.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = CompositeUserTypeGenericArrayTest.Population.class )
@SessionFactory
@JiraKey( "HHH-13560" )
class CompositeUserTypeGenericArrayTest {

	@Test
	void genericArrayAndVoidSetters(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Matrix matrix = new Matrix();
			matrix.name = "effective";
			matrix.dimNames = List.of( "rows", "columns" );
			matrix.setSemantics( new List<?>[] { List.of( "r1", "r2" ), List.of( 1, 2 ) } );
			matrix.data = new int[] { 10, 20, 30, 40 };
			Population population = new Population();
			population.id = 1;
			population.effective = matrix;
			session.persist( population );
		} );
		scope.inTransaction( session -> {
			Population population = session.createQuery(
					"from MatrixPopulation p where p.effective.name = :name", Population.class )
					.setParameter( "name", "effective" ).getSingleResult();
			assertThat( population.loaded ).isTrue();
			assertThat( population.effective.dimNames ).containsExactly( "rows", "columns" );
			assertThat( population.effective.getSemantics()[0] ).isEqualTo( List.of( "r1", "r2" ) );
			assertThat( population.effective.getSemantics()[1] ).isEqualTo( List.of( 1, 2 ) );
			assertThat( (int[]) population.effective.data ).containsExactly( 10, 20, 30, 40 );
			population.effective.setSemantics( new List<?>[] { List.of( "updated" ) } );
		} );
		scope.inTransaction( session -> {
			Population population = session.find( Population.class, 1 );
			assertThat( population.effective.getSemantics() ).hasSize( 1 );
			assertThat( population.effective.getSemantics()[0] ).isEqualTo( List.of( "updated" ) );
		} );
	}

	@Entity( name = "MatrixPopulation" )
	public static class Population {
		@Id
		Integer id;
		@Embedded
		@CompositeType( MatrixType.class )
		Matrix effective;
		@Transient
		boolean loaded;

		@PostLoad
		void loaded() {
			loaded = true;
		}
	}

	public static class Matrix implements Serializable {
		protected String name;
		protected List<String> dimNames;
		protected List<?>[] semantics;
		protected Object data;

		public List<?>[] getSemantics() {
			return semantics;
		}

		public void setSemantics(List<?>[] semantics) {
			this.semantics = semantics;
		}
	}

	public static class MatrixProjection {
		String name;
		@Column( length = 10000 )
		byte[] payload;
	}

	public static class MatrixType implements CompositeUserType<Matrix> {
		@Override
		public Object getPropertyValue(Matrix component, int property) {
			return switch ( property ) {
				case 0 -> component.name;
				case 1 -> SerializationHelper.serialize( component );
				default -> throw new IllegalArgumentException( "Unknown property: " + property );
			};
		}

		@Override
		public Matrix instantiate(ValueAccess values) {
			byte[] payload = values.getValue( 1, byte[].class );
			return payload == null ? null : (Matrix) SerializationHelper.deserialize( payload );
		}

		@Override
		public Class<?> embeddable() {
			return MatrixProjection.class;
		}

		@Override
		public Class<Matrix> returnedClass() {
			return Matrix.class;
		}

		@Override
		public boolean equals(Matrix x, Matrix y) {
			return x == y || x != null && y != null
					&& Objects.equals( x.name, y.name ) && Objects.equals( x.dimNames, y.dimNames )
					&& Objects.deepEquals( x.semantics, y.semantics ) && Objects.deepEquals( x.data, y.data );
		}

		@Override
		public int hashCode(Matrix value) {
			return Objects.hash( value.name, value.dimNames );
		}

		@Override
		public Matrix deepCopy(Matrix value) {
			return value == null ? null : (Matrix) SerializationHelper.clone( value );
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Serializable disassemble(Matrix value) {
			return value == null ? null : SerializationHelper.serialize( value );
		}

		@Override
		public Matrix assemble(Serializable cached, Object owner) {
			return cached == null ? null : (Matrix) SerializationHelper.deserialize( (byte[]) cached );
		}

		@Override
		public Matrix replace(Matrix detached, Matrix managed, Object owner) {
			return deepCopy( detached );
		}
	}
}
