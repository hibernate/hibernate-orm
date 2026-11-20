package org.hibernate.vector;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.vector.VectorTestHelper.cosineDistance;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Passing a JPA Criteria parameter created for {@code float[]} to a vector
 * distance function should work, i.e. the parameter type should be inferred
 * from the vector typed argument, instead of eagerly resolving the basic type
 * for {@code float[]} (which is not a vector type and hence fails validation in
 * {@link org.hibernate.vector.internal.VectorArgumentValidator}).
 * <p>
 * Note that the problem only shows when a basic type is registered for the
 * Java type {@code float[]}, which happens e.g. when the domain model contains
 * a plain (non-vector) {@code float[]} attribute that is mapped as an array.
 */
@DomainModel(annotatedClasses = CriteriaVectorParameterTest.VectorEntity.class)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsVectorType.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsCosineDistance.class)
@SkipForDialect(dialectClass = PostgresPlusDialect.class, reason = "Test database does not have the extension enabled")
@SkipForDialect(dialectClass = MySQLDialect.class, reason = "Only MySQL HeatWave supports this function")
@JiraKey("HHH-20114")
public class CriteriaVectorParameterTest {

	private static final float[] V1 = new float[] { 1, 2, 3 };
	private static final float[] V2 = new float[] { 4, 5, 6 };

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new VectorEntity( 1L, V1 ) );
			em.persist( new VectorEntity( 2L, V2 ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testCosineDistanceWithCriteriaParameter(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final float[] vector = new float[] { 1, 1, 1 };

			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<Tuple> query = cb.createTupleQuery();
			final Root<VectorEntity> root = query.from( VectorEntity.class );
			final ParameterExpression<float[]> vec = cb.parameter( float[].class, "vec" );
			// Fails here with FunctionArgumentException:
			// "Parameter 1 of function 'cosine_distance()' requires a vector type, but argument is of type 'float[]'"
			final Expression<Double> distance = cb.function(
					"cosine_distance",
					Double.class,
					root.get( "theVector" ),
					vec
			);
			query.multiselect( root.get( "id" ), distance ).orderBy( cb.asc( root.get( "id" ) ) );

			final List<Tuple> results = em.createQuery( query )
					.setParameter( vec, vector )
					.getResultList();

			assertEquals( 2, results.size() );
			assertEquals( 1L, results.get( 0 ).get( 0 ) );
			assertEquals( cosineDistance( V1, vector ), results.get( 0 ).get( 1, double.class ), 0.0000001D );
			assertEquals( 2L, results.get( 1 ).get( 0 ) );
			assertEquals( cosineDistance( V2, vector ), results.get( 1 ).get( 1, double.class ), 0.0000001D );
		} );
	}

	@Entity(name = "VectorEntity")
	public static class VectorEntity {

		@Id
		private Long id;

		@Column(name = "the_vector")
		@JdbcTypeCode(SqlTypes.VECTOR)
		@Array(length = 3)
		private float[] theVector;

		// A plain float[] attribute causes an array basic type to be registered for float[]
		private float[] plainArray;

		public VectorEntity() {
		}

		public VectorEntity(Long id, float[] theVector) {
			this.id = id;
			this.theVector = theVector;
		}
	}
}
