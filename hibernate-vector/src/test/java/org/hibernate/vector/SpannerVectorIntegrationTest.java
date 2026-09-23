/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.vector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.DoublePrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.FloatPrimitiveArrayJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.vector.internal.VectorHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.util.List;
import java.util.ServiceLoader;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.vector.internal.SpannerPostgreSQLVectorJdbcType;
import org.hibernate.vector.internal.SpannerVectorJdbcType;

import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.vector.internal.SpannerPostgreSQLVectorDdlType;
import org.hibernate.vector.internal.SpannerPostgreSQLVectorFunctionContributor;
import org.hibernate.vector.internal.SpannerPostgreSQLVectorTypeContributor;
import org.hibernate.vector.internal.SpannerVectorDdlType;
import org.hibernate.vector.internal.SpannerVectorFunctionContributor;
import org.hibernate.vector.internal.SpannerVectorTypeContributor;

import static org.hibernate.vector.VectorTestHelper.cosineDistance;
import static org.hibernate.vector.VectorTestHelper.euclideanDistance;
import static org.hibernate.vector.VectorTestHelper.euclideanNorm;
import static org.hibernate.vector.VectorTestHelper.euclideanSquaredDistance;
import static org.hibernate.vector.VectorTestHelper.innerProduct;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		SpannerVectorIntegrationTest.DimensionedVectorEntity.class,
		SpannerVectorIntegrationTest.UnconstrainedVectorEntity.class,
		SpannerVectorIntegrationTest.BoxedVectorEntity.class
})
@SessionFactory
@RequiresDialect(SpannerDialect.class)
@RequiresDialect(SpannerPostgreSQLDialect.class)
public class SpannerVectorIntegrationTest {

	private static final float[] V1 = new float[] { 1.0f, 2.0f, 3.0f };
	private static final float[] V2 = new float[] { 4.0f, 5.0f, 6.0f };
	private static final double[] D1 = new double[] { 1.0d, 2.0d, 3.0d };
	private static final double[] D2 = new double[] { 4.0d, 5.0d, 6.0d };

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new DimensionedVectorEntity( 1L, V1, V1, D1 ) );
			em.persist( new DimensionedVectorEntity( 2L, V2, V2, D2 ) );

			em.persist( new UnconstrainedVectorEntity( 1L, new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, new double[] { 10.0d, 20.0d } ) );

			em.persist( new BoxedVectorEntity( 1L, new Float[] { 1.0f, 2.0f, 3.0f }, new Double[] { 1.0d, 2.0d, 3.0d } ) );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.createMutationQuery( "delete from DimensionedVectorEntity" ).executeUpdate();
			em.createMutationQuery( "delete from UnconstrainedVectorEntity" ).executeUpdate();
			em.createMutationQuery( "delete from BoxedVectorEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testDimensionedVectorsReadWrite(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			DimensionedVectorEntity entity = em.find( DimensionedVectorEntity.class, 1L );
			assertNotNull( entity );
			assertArrayEquals( V1, entity.getVFloat() );
			assertArrayEquals( V1, entity.getVFloat32() );
			assertArrayEquals( D1, entity.getVFloat64() );

			DimensionedVectorEntity entity2 = em.find( DimensionedVectorEntity.class, 2L );
			assertNotNull( entity2 );
			assertArrayEquals( V2, entity2.getVFloat() );
			assertArrayEquals( V2, entity2.getVFloat32() );
			assertArrayEquals( D2, entity2.getVFloat64() );
		} );
	}

	@Test
	public void testUnconstrainedVectorsReadWrite(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			UnconstrainedVectorEntity entity = em.find( UnconstrainedVectorEntity.class, 1L );
			assertNotNull( entity );
			assertArrayEquals( new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, entity.getVUnconstrained() );
			assertArrayEquals( new double[] { 10.0d, 20.0d }, entity.getVUnconstrainedDouble() );
		} );
	}

	@Test
	public void testBoxedVectorsReadWrite(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			BoxedVectorEntity entity = em.find( BoxedVectorEntity.class, 1L );
			assertNotNull( entity );
			assertArrayEquals( new Float[] { 1.0f, 2.0f, 3.0f }, entity.getVBoxedFloat() );
			assertArrayEquals( new Double[] { 1.0d, 2.0d, 3.0d }, entity.getVBoxedDouble() );
		} );
	}

	@Test
	public void testNullVectorsReadWrite(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			em.persist( new DimensionedVectorEntity( 99L, null, null, null ) );
		} );
		scope.inTransaction( em -> {
			DimensionedVectorEntity entity = em.find( DimensionedVectorEntity.class, 99L );
			assertNotNull( entity );
			assertNull( entity.getVFloat() );
			assertNull( entity.getVFloat32() );
			assertNull( entity.getVFloat64() );
		} );
	}

	@Test
	public void testDistanceAndNormFunctions(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final float[] queryVec = new float[] { 1.0f, 1.0f, 1.0f };
			final List<Tuple> results = em.createSelectionQuery(
							"select e.id, " +
									"cosine_distance(e.vFloat, :vec), " +
									"euclidean_distance(e.vFloat, :vec), " +
									"euclidean_squared_distance(e.vFloat, :vec), " +
									"inner_product(e.vFloat, :vec), " +
									"negative_inner_product(e.vFloat, :vec), " +
									"vector_dims(e.vFloat), " +
									"vector_norm(e.vFloat), " +
									"l2_norm(e.vFloat) " +
									"from DimensionedVectorEntity e order by e.id",
							Tuple.class
					)
					.setParameter( "vec", queryVec )
					.getResultList();

			assertEquals( 2, results.size() );

			Tuple row1 = results.get( 0 );
			assertEquals( 1L, row1.get( 0 ) );
			assertEquals( cosineDistance( V1, queryVec ), row1.get( 1, double.class ), 0.00001D );
			assertEquals( euclideanDistance( V1, queryVec ), row1.get( 2, double.class ), 0.00001D );
			assertEquals( euclideanSquaredDistance( V1, queryVec ), row1.get( 3, double.class ), 0.00001D );
			assertEquals( innerProduct( V1, queryVec ), row1.get( 4, double.class ), 0.00001D );
			assertEquals( innerProduct( V1, queryVec ) * -1, row1.get( 5, double.class ), 0.00001D );
			assertEquals( 3, row1.get( 6 ) );
			assertEquals( euclideanNorm( V1 ), row1.get( 7, double.class ), 0.00001D );
			assertEquals( euclideanNorm( V1 ), row1.get( 8, double.class ), 0.00001D );
		} );
	}

	@Test
	public void testCastPatterns(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Tuple tuple = em.createSelectionQuery(
							"select cast(e.vFloat as string), " +
									"cast('[1, 1, 1]' as vector(3)), " +
									"cast('[2, 2, 2]' as float_vector(3)), " +
									"cast('[3, 3, 3]' as double_vector(3)) " +
									"from DimensionedVectorEntity e where e.id = 1",
							Tuple.class
					)
					.getSingleResult();

			assertNotNull( tuple.get( 0, String.class ) );
			assertArrayEquals( new float[] { 1.0f, 2.0f, 3.0f }, VectorHelper.parseFloatVector( tuple.get( 0, String.class ) ) );
			assertArrayEquals( new float[] { 1.0f, 1.0f, 1.0f }, tuple.get( 1, float[].class ) );
			assertArrayEquals( new float[] { 2.0f, 2.0f, 2.0f }, tuple.get( 2, float[].class ) );
			assertArrayEquals( new double[] { 3.0d, 3.0d, 3.0d }, tuple.get( 3, double[].class ) );
		} );
	}

	@Test
	public void testDoubleVectorDistances(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final double[] queryVec = new double[] { 1.0d, 1.0d, 1.0d };
			final List<Tuple> results = em.createSelectionQuery(
							"select e.id, " +
									"cosine_distance(e.vFloat64, :vec), " +
									"euclidean_distance(e.vFloat64, :vec), " +
									"inner_product(e.vFloat64, :vec), " +
									"vector_dims(e.vFloat64), " +
									"vector_norm(e.vFloat64) " +
									"from DimensionedVectorEntity e order by e.id",
							Tuple.class
					)
					.setParameter( "vec", queryVec )
					.getResultList();

			assertEquals( 2, results.size() );
			Tuple row1 = results.get( 0 );
			assertEquals( 1L, row1.get( 0 ) );
			assertEquals( cosineDistance( new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 1.0f, 1.0f, 1.0f } ), row1.get( 1, double.class ), 0.00001D );
			assertEquals( euclideanDistance( new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 1.0f, 1.0f, 1.0f } ), row1.get( 2, double.class ), 0.00001D );
			assertEquals( innerProduct( new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 1.0f, 1.0f, 1.0f } ), row1.get( 3, double.class ), 0.00001D );
			assertEquals( 3, row1.get( 4 ) );
			assertEquals( euclideanNorm( new float[] { 1.0f, 2.0f, 3.0f } ), row1.get( 5, double.class ), 0.00001D );
		} );
	}

	@Test
	public void testLiteralFormattingAgainstDatabase(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
			WrapperOptions wrapperOptions = scope.getSessionFactory().getWrapperOptions();

			// 1. Float primitive array literal formatting
			JdbcType floatJdbcType = scope.getSessionFactory().getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.VECTOR );
			JdbcLiteralFormatter<float[]> floatFormatter = floatJdbcType.getJdbcLiteralFormatter( FloatPrimitiveArrayJavaType.INSTANCE );
			SqlAppender floatAppender = new StringBuilderSqlAppender();
			floatFormatter.appendJdbcLiteral( floatAppender, new float[] { 1.0f, 2.0f, 3.0f }, dialect, wrapperOptions );
			String floatLiteralSql = floatAppender.toString();
			Object floatResult = em.createNativeQuery( "SELECT " + floatLiteralSql ).getSingleResult();
			assertNotNull( floatResult );

			// 2. Double primitive array literal formatting
			JdbcType doubleJdbcType = scope.getSessionFactory().getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.VECTOR_FLOAT64 );
			JdbcLiteralFormatter<double[]> doubleFormatter = doubleJdbcType.getJdbcLiteralFormatter( DoublePrimitiveArrayJavaType.INSTANCE );
			SqlAppender doubleAppender = new StringBuilderSqlAppender();
			doubleFormatter.appendJdbcLiteral( doubleAppender, new double[] { 1.0d, 2.0d, 3.0d }, dialect, wrapperOptions );
			String doubleLiteralSql = doubleAppender.toString();
			Object doubleResult = em.createNativeQuery( "SELECT " + doubleLiteralSql ).getSingleResult();
			assertNotNull( doubleResult );

			// 3. Boxed Float array literal formatting
			JavaType<Float[]> boxedFloatJavaType = scope.getSessionFactory().getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( Float[].class );
			JdbcLiteralFormatter<Float[]> boxedFloatFormatter = floatJdbcType.getJdbcLiteralFormatter( boxedFloatJavaType );
			SqlAppender boxedFloatAppender = new StringBuilderSqlAppender();
			boxedFloatFormatter.appendJdbcLiteral( boxedFloatAppender, new Float[] { 1.0f, 2.0f, 3.0f }, dialect, wrapperOptions );
			String boxedFloatLiteralSql = boxedFloatAppender.toString();
			Object boxedFloatResult = em.createNativeQuery( "SELECT " + boxedFloatLiteralSql ).getSingleResult();
			assertNotNull( boxedFloatResult );
		} );
	}

	@Test
	public void testApproximateDistanceFunctions(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		if ( dialect instanceof SpannerPostgreSQLDialect ) {
			// Cloud Spanner Emulator (1.5.57) does not support spanner.approx_*_distance ANN functions in PostgreSQL dialect yet
			return;
		}

		scope.inTransaction( em -> {
			em.createNativeQuery(
					"CREATE VECTOR INDEX idx_dve_cosine ON DimensionedVectorEntity(v_float) " +
							"WHERE v_float IS NOT NULL OPTIONS (distance_type = 'COSINE')"
			).executeUpdate();
			em.createNativeQuery(
					"CREATE VECTOR INDEX idx_dve_euclidean ON DimensionedVectorEntity(v_float) " +
							"WHERE v_float IS NOT NULL OPTIONS (distance_type = 'EUCLIDEAN')"
			).executeUpdate();
			em.createNativeQuery(
					"CREATE VECTOR INDEX idx_dve_dot_product ON DimensionedVectorEntity(v_float) " +
							"WHERE v_float IS NOT NULL OPTIONS (distance_type = 'DOT_PRODUCT')"
			).executeUpdate();
		} );

		try {
			scope.inTransaction( em -> {
				final float[] queryVec = new float[] { 1.0f, 1.0f, 1.0f };

				// 1. approx_cosine_distance (2-argument with default options, and 3-argument with custom options)
				final List<Long> cosineResults = em.createSelectionQuery(
								"select e.id from DimensionedVectorEntity e " +
										"where e.vFloat is not null " +
										"order by approx_cosine_distance(e.vFloat, :vec) limit 1",
								Long.class
						)
						.setParameter( "vec", queryVec )
						.getResultList();

				assertEquals( 1, cosineResults.size() );
				assertEquals( 2L, cosineResults.get( 0 ) );

				final List<Long> customCosineResults = em.createSelectionQuery(
								"select e.id from DimensionedVectorEntity e " +
										"where e.vFloat is not null " +
										"order by approx_cosine_distance(e.vFloat, :vec, 150) limit 1",
								Long.class
						)
						.setParameter( "vec", queryVec )
						.getResultList();

				assertEquals( 1, customCosineResults.size() );
				assertEquals( 2L, customCosineResults.get( 0 ) );

				// 2. approx_euclidean_distance (2-arg and 3-arg)
				final List<Long> euclideanResults = em.createSelectionQuery(
								"select e.id from DimensionedVectorEntity e " +
										"where e.vFloat is not null " +
										"order by approx_euclidean_distance(e.vFloat, :vec) limit 1",
								Long.class
						)
						.setParameter( "vec", queryVec )
						.getResultList();

				assertEquals( 1, euclideanResults.size() );
				assertEquals( 1L, euclideanResults.get( 0 ) );

				final List<Long> customEuclideanResults = em.createSelectionQuery(
								"select e.id from DimensionedVectorEntity e " +
										"where e.vFloat is not null " +
										"order by approx_euclidean_distance(e.vFloat, :vec, 150) limit 1",
								Long.class
						)
						.setParameter( "vec", queryVec )
						.getResultList();

				assertEquals( 1, customEuclideanResults.size() );
				assertEquals( 1L, customEuclideanResults.get( 0 ) );

				// 3. approx_dot_product (2-arg and 3-arg; sorted descending by similarity)
				final List<Long> dotResults = em.createSelectionQuery(
								"select e.id from DimensionedVectorEntity e " +
										"where e.vFloat is not null " +
										"order by approx_dot_product(e.vFloat, :vec) desc limit 1",
								Long.class
						)
						.setParameter( "vec", queryVec )
						.getResultList();

				assertEquals( 1, dotResults.size() );
				assertEquals( 2L, dotResults.get( 0 ) );

				final List<Long> customDotResults = em.createSelectionQuery(
								"select e.id from DimensionedVectorEntity e " +
										"where e.vFloat is not null " +
										"order by approx_dot_product(e.vFloat, :vec, 150) desc limit 1",
								Long.class
						)
						.setParameter( "vec", queryVec )
						.getResultList();

				assertEquals( 1, customDotResults.size() );
				assertEquals( 2L, customDotResults.get( 0 ) );
			} );
		}
		finally {
			scope.inTransaction( em -> {
				try {
					em.createNativeQuery( "DROP VECTOR INDEX idx_dve_cosine" ).executeUpdate();
				}
				catch (Exception ignored) {
				}
				try {
					em.createNativeQuery( "DROP VECTOR INDEX idx_dve_euclidean" ).executeUpdate();
				}
				catch (Exception ignored) {
				}
				try {
					em.createNativeQuery( "DROP VECTOR INDEX idx_dve_dot_product" ).executeUpdate();
				}
				catch (Exception ignored) {
				}
			} );
		}
	}

	@Test
	public void testVectorIndexDdl(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Exporter<org.hibernate.mapping.Index> indexExporter = dialect.getIndexExporter();

		final org.hibernate.mapping.Table table = new org.hibernate.mapping.Table( "orm", "items" );
		final org.hibernate.mapping.Column vectorCol = new org.hibernate.mapping.Column( "embedding" );
		vectorCol.setSqlTypeCode( SqlTypes.VECTOR );
		table.addColumn( vectorCol );

		final org.hibernate.mapping.Index vectorIndex = new org.hibernate.mapping.Index();
		vectorIndex.setName( "idx_items_embedding" );
		vectorIndex.setTable( table );
		vectorIndex.addColumn( vectorCol );

		final SqlStringGenerationContext context = SqlStringGenerationContextImpl.forTests(
				scope.getSessionFactory().getJdbcServices().getJdbcEnvironment()
		);

		final String[] createSql = indexExporter.getSqlCreateStrings( vectorIndex, null, context );
		final String[] dropSql = indexExporter.getSqlDropStrings( vectorIndex, null, context );

		if ( dialect instanceof SpannerPostgreSQLDialect ) {
			assertEquals( 1, createSql.length );
			assertEquals( "create index idx_items_embedding on items using scann (embedding) with (distance_type = 'COSINE') where (embedding is not null)", createSql[0] );
			assertEquals( 1, dropSql.length );
			assertEquals( "drop index idx_items_embedding", dropSql[0] );
		}
		else if ( dialect instanceof SpannerDialect ) {
			assertEquals( 1, createSql.length );
			assertEquals( "create vector index idx_items_embedding on items (embedding) where embedding is not null options (distance_type = 'COSINE')", createSql[0] );
			assertEquals( 1, dropSql.length );
			assertEquals( "drop vector index idx_items_embedding", dropSql[0] );

			// Verify SpannerDialectTableExporter drops vector indexes with 'drop vector index if exists'
			final Exporter<org.hibernate.mapping.Table> tableExporter = dialect.getTableExporter();
			table.addIndex( vectorIndex );
			final String[] tableDropSql = tableExporter.getSqlDropStrings( table, null, context );
			boolean foundVectorDrop = false;
			for ( String stmt : tableDropSql ) {
				if ( stmt.startsWith( "drop vector index if exists idx_items_embedding" ) ) {
					foundVectorDrop = true;
					break;
				}
			}
			assertTrue( foundVectorDrop, "TableExporter must emit 'drop vector index if exists' for vector index" );
		}

		// Verify non-vector index continues to delegate to standard index exporter
		final org.hibernate.mapping.Column standardCol = new org.hibernate.mapping.Column( "name" );
		standardCol.setSqlTypeCode( SqlTypes.VARCHAR );
		final org.hibernate.mapping.Index standardIndex = new org.hibernate.mapping.Index();
		standardIndex.setName( "idx_items_name" );
		standardIndex.setTable( table );
		standardIndex.addColumn( standardCol );

		final String[] standardCreateSql = indexExporter.getSqlCreateStrings( standardIndex, null, context );
		final String[] standardDropSql = indexExporter.getSqlDropStrings( standardIndex, null, context );
		assertEquals( 1, standardCreateSql.length );
		assertEquals( "create index idx_items_name on items (name)", standardCreateSql[0] );
		assertEquals( 1, standardDropSql.length );
		assertEquals( "drop index idx_items_name", standardDropSql[0] );
	}

	@Test
	public void testVectorDdlType(SessionFactoryScope scope) {
		final Dialect dialect = scope.getSessionFactory().getJdbcServices().getDialect();
		final Size size128 = new Size();
		size128.setArrayLength( 128 );

		if ( dialect instanceof SpannerPostgreSQLDialect ) {
			final SpannerPostgreSQLVectorDdlType pgDdl =
					new SpannerPostgreSQLVectorDdlType( SqlTypes.VECTOR, "float4", dialect );
			assertEquals( "float4[] VECTOR LENGTH 128", pgDdl.getTypeName( size128, null, null ) );
			assertEquals( "float4[]", pgDdl.getTypeName( new Size(), null, null ) );
			assertEquals( "float4[]", pgDdl.getCastTypeName( size128, null, null ) );
		}
		else if ( dialect instanceof SpannerDialect ) {
			final SpannerVectorDdlType gsqlDdl =
					new SpannerVectorDdlType( SqlTypes.VECTOR, "FLOAT32", dialect );
			assertEquals( "ARRAY<FLOAT32>(vector_length=>128)", gsqlDdl.getTypeName( size128, null, null ) );
			assertEquals( "ARRAY<FLOAT32>", gsqlDdl.getTypeName( new Size(), null, null ) );
			assertEquals( "ARRAY<FLOAT32>", gsqlDdl.getCastTypeName( size128, null, null ) );
		}
	}

	@Test
	public void testSpannerPostgreSQLVectorIndexDdl() {
		final SpannerPostgreSQLDialect pgDialect = new SpannerPostgreSQLDialect();
		final org.hibernate.dialect.schema.internal.SpannerPostgreSQLIndexExporter pgExporter =
				new org.hibernate.dialect.schema.internal.SpannerPostgreSQLIndexExporter( pgDialect );

		final org.hibernate.mapping.Table table = new org.hibernate.mapping.Table( "orm", "items" );
		final org.hibernate.mapping.Column nullableVectorCol = new org.hibernate.mapping.Column( "embedding" );
		nullableVectorCol.setSqlTypeCode( SqlTypes.VECTOR );
		nullableVectorCol.setNullable( true );
		table.addColumn( nullableVectorCol );

		final org.hibernate.mapping.Index nullableIndex = new org.hibernate.mapping.Index();
		nullableIndex.setName( "idx_items_nullable" );
		nullableIndex.setTable( table );
		nullableIndex.addColumn( nullableVectorCol );

		final String[] nullableSql = pgExporter.getSqlCreateStrings( nullableIndex, null, null );
		assertEquals( 1, nullableSql.length );
		assertEquals( "create index idx_items_nullable on items using scann (embedding) with (distance_type = 'COSINE') where (embedding is not null)", nullableSql[0] );

		final org.hibernate.mapping.Column nonNullableVectorCol = new org.hibernate.mapping.Column( "vec_nonnull" );
		nonNullableVectorCol.setSqlTypeCode( SqlTypes.VECTOR );
		nonNullableVectorCol.setNullable( false );
		table.addColumn( nonNullableVectorCol );

		final org.hibernate.mapping.Index nonNullableIndex = new org.hibernate.mapping.Index();
		nonNullableIndex.setName( "idx_items_nonnull" );
		nonNullableIndex.setTable( table );
		nonNullableIndex.addColumn( nonNullableVectorCol );

		final String[] nonNullSql = pgExporter.getSqlCreateStrings( nonNullableIndex, null, null );
		assertEquals( 1, nonNullSql.length );
		assertEquals( "create index idx_items_nonnull on items using scann (vec_nonnull) with (distance_type = 'COSINE')", nonNullSql[0] );

		final org.hibernate.mapping.Index orphanIndex = new org.hibernate.mapping.Index();
		orphanIndex.setName( "idx_orphan" );
		orphanIndex.addColumn( nullableVectorCol );
		assertThrows( IllegalArgumentException.class, () -> pgExporter.getSqlCreateStrings( orphanIndex, null, null ) );

		final org.hibernate.dialect.schema.internal.SpannerIndexExporter gsqlExporter =
				new org.hibernate.dialect.schema.internal.SpannerIndexExporter( new SpannerDialect() );
		assertThrows( IllegalArgumentException.class, () -> gsqlExporter.getSqlCreateStrings( orphanIndex, null, null ) );

		final org.hibernate.mapping.Index usingVectorIndex = new org.hibernate.mapping.Index();
		usingVectorIndex.setName( "idx_using_vec" );
		usingVectorIndex.setUsing( "vector" );
		assertTrue( gsqlExporter.isVectorIndex( usingVectorIndex, null ) );
	}

	@Test
	public void testVectorSpiDiscovery() {
		final ClassLoader classLoader = getClass().getClassLoader();
		final ServiceLoader<TypeContributor> typeLoader = ServiceLoader.load( TypeContributor.class, classLoader );
		final List<Class<? extends TypeContributor>> typeClasses = typeLoader.stream()
				.map( ServiceLoader.Provider::type )
				.toList();
		assertTrue( typeClasses.contains( SpannerVectorTypeContributor.class ) );
		assertTrue( typeClasses.contains( SpannerPostgreSQLVectorTypeContributor.class ) );

		final ServiceLoader<FunctionContributor> fnLoader = ServiceLoader.load( FunctionContributor.class, classLoader );
		final List<Class<? extends FunctionContributor>> fnClasses = fnLoader.stream()
				.map( ServiceLoader.Provider::type )
				.toList();
		assertTrue( fnClasses.contains( SpannerVectorFunctionContributor.class ) );
		assertTrue( fnClasses.contains( SpannerPostgreSQLVectorFunctionContributor.class ) );
	}

	private static ResultSet mockResultSet(Object returnedValue) {
		return (ResultSet) Proxy.newProxyInstance(
				ResultSet.class.getClassLoader(),
				new Class<?>[] { ResultSet.class },
				(proxy, method, args) -> {
					if ( "getObject".equals( method.getName() ) ) {
						return returnedValue;
					}
					if ( "getArray".equals( method.getName() ) ) {
						return returnedValue instanceof java.sql.Array ? returnedValue : null;
					}
					if ( "wasNull".equals( method.getName() ) ) {
						return returnedValue == null;
					}
					return null;
				}
		);
	}

	@Test
	public void testVectorExtractors() throws Exception {
		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final JavaType<float[]> floatArrayType = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( float[].class );
		final JavaType<Float[]> boxedFloatArrayType = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Float[].class );
		final JavaType<double[]> doubleArrayType = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( double[].class );
		final JavaType<Double[]> boxedDoubleArrayType = typeConfiguration.getJavaTypeRegistry().resolveDescriptor( Double[].class );

		final SpannerPostgreSQLVectorJdbcType pgFloatVector = new SpannerPostgreSQLVectorJdbcType(
				SqlTypes.VECTOR, "float4", typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.FLOAT )
		);
		final SpannerPostgreSQLVectorJdbcType pgDoubleVector = new SpannerPostgreSQLVectorJdbcType(
				SqlTypes.VECTOR_FLOAT64, "float8", typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE )
		);

		final SpannerVectorJdbcType gsqlFloatVector = new SpannerVectorJdbcType(
				typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.FLOAT ), SqlTypes.VECTOR, "FLOAT32"
		);
		final SpannerVectorJdbcType gsqlDoubleVector = new SpannerVectorJdbcType(
				typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.DOUBLE ), SqlTypes.VECTOR_FLOAT64, "FLOAT64"
		);

		final ValueExtractor<float[]> pgFloatToPrimitiveExtractor = pgFloatVector.getExtractor( floatArrayType );
		final ValueExtractor<Float[]> pgFloatToBoxedExtractor = pgFloatVector.getExtractor( boxedFloatArrayType );
		final ValueExtractor<double[]> pgDoubleToPrimitiveExtractor = pgDoubleVector.getExtractor( doubleArrayType );
		final ValueExtractor<Double[]> pgDoubleToBoxedExtractor = pgDoubleVector.getExtractor( boxedDoubleArrayType );

		// 1. Primitive and boxed float[] extractions
		assertArrayEquals( new float[] { 1.0f, 2.0f }, pgFloatToPrimitiveExtractor.extract( mockResultSet( new float[] { 1.0f, 2.0f } ), 1, null ) );
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, pgFloatToBoxedExtractor.extract( mockResultSet( new float[] { 1.0f, 2.0f } ), 1, null ) );
		assertArrayEquals( new float[] { 1.0f, 2.0f }, pgFloatToPrimitiveExtractor.extract( mockResultSet( new Float[] { 1.0f, 2.0f } ), 1, null ) );
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, pgFloatToBoxedExtractor.extract( mockResultSet( new Float[] { 1.0f, 2.0f } ), 1, null ) );

		// 2. String representation extractions (PostgreSQL and GoogleSQL formats)
		assertArrayEquals( new float[] { 1.0f, 2.0f }, pgFloatToPrimitiveExtractor.extract( mockResultSet( "{1.0, 2.0}" ), 1, null ) );
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, pgFloatToBoxedExtractor.extract( mockResultSet( "{1.0, 2.0}" ), 1, null ) );
		assertArrayEquals( new float[] { 1.0f, 2.0f }, pgFloatToPrimitiveExtractor.extract( mockResultSet( "[1.0, 2.0]" ), 1, null ) );
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, pgFloatToBoxedExtractor.extract( mockResultSet( "[1.0, 2.0]" ), 1, null ) );

		// 3. Double vector extractions
		assertArrayEquals( new double[] { 1.0d, 2.0d }, pgDoubleToPrimitiveExtractor.extract( mockResultSet( new double[] { 1.0d, 2.0d } ), 1, null ) );
		assertArrayEquals( new Double[] { 1.0d, 2.0d }, pgDoubleToBoxedExtractor.extract( mockResultSet( new double[] { 1.0d, 2.0d } ), 1, null ) );
		assertArrayEquals( new double[] { 1.0d, 2.0d }, pgDoubleToPrimitiveExtractor.extract( mockResultSet( "{1.0, 2.0}" ), 1, null ) );
		assertArrayEquals( new Double[] { 1.0d, 2.0d }, pgDoubleToBoxedExtractor.extract( mockResultSet( "{1.0, 2.0}" ), 1, null ) );

		// 4. Cross-type primitive bridging (double[] to Float[], float[] to Double[])
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, pgFloatToBoxedExtractor.extract( mockResultSet( new double[] { 1.0d, 2.0d } ), 1, null ) );
		assertArrayEquals( new Double[] { 1.0d, 2.0d }, pgDoubleToBoxedExtractor.extract( mockResultSet( new float[] { 1.0f, 2.0f } ), 1, null ) );

		// 5. GoogleSQL vector extractions
		final ValueExtractor<float[]> gsqlFloatToPrimitiveExtractor = gsqlFloatVector.getExtractor( floatArrayType );
		final ValueExtractor<Float[]> gsqlFloatToBoxedExtractor = gsqlFloatVector.getExtractor( boxedFloatArrayType );
		assertArrayEquals( new float[] { 1.0f, 2.0f }, gsqlFloatToPrimitiveExtractor.extract( mockResultSet( new float[] { 1.0f, 2.0f } ), 1, null ) );
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, gsqlFloatToBoxedExtractor.extract( mockResultSet( new float[] { 1.0f, 2.0f } ), 1, null ) );
		assertArrayEquals( new float[] { 1.0f, 2.0f }, gsqlFloatToPrimitiveExtractor.extract( mockResultSet( "[1.0, 2.0]" ), 1, null ) );
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, gsqlFloatToBoxedExtractor.extract( mockResultSet( "[1.0, 2.0]" ), 1, null ) );

		// 6. java.sql.Array extraction via getArray
		final boolean[] freed = new boolean[1];
		final java.sql.Array mockArray = (java.sql.Array) Proxy.newProxyInstance(
				java.sql.Array.class.getClassLoader(),
				new Class<?>[] { java.sql.Array.class },
				(proxy, method, args) -> {
					if ( "getArray".equals( method.getName() ) ) {
						return new float[] { 1.0f, 2.0f };
					}
					if ( "free".equals( method.getName() ) ) {
						freed[0] = true;
						return null;
					}
					return null;
				}
		);
		assertArrayEquals( new float[] { 1.0f, 2.0f }, pgFloatToPrimitiveExtractor.extract( mockResultSet( mockArray ), 1, null ) );
		assertTrue( freed[0] );
		freed[0] = false;
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, pgFloatToBoxedExtractor.extract( mockResultSet( mockArray ), 1, null ) );
		assertTrue( freed[0] );
		freed[0] = false;
		assertArrayEquals( new float[] { 1.0f, 2.0f }, gsqlFloatToPrimitiveExtractor.extract( mockResultSet( mockArray ), 1, null ) );
		assertTrue( freed[0] );
		freed[0] = false;
		assertArrayEquals( new Float[] { 1.0f, 2.0f }, gsqlFloatToBoxedExtractor.extract( mockResultSet( mockArray ), 1, null ) );
		assertTrue( freed[0] );

		// 7. Null extractions
		assertNull( pgFloatToPrimitiveExtractor.extract( mockResultSet( null ), 1, null ) );
		assertNull( pgFloatToBoxedExtractor.extract( mockResultSet( null ), 1, null ) );
		assertNull( gsqlFloatToPrimitiveExtractor.extract( mockResultSet( null ), 1, null ) );
		assertNull( gsqlFloatToBoxedExtractor.extract( mockResultSet( null ), 1, null ) );
	}

	@Entity(name = "DimensionedVectorEntity")
	public static class DimensionedVectorEntity {
		@Id
		private Long id;

		@Column(name = "v_float")
		@JdbcTypeCode(SqlTypes.VECTOR)
		@Array(length = 3)
		private float[] vFloat;

		@Column(name = "v_float32")
		@JdbcTypeCode(SqlTypes.VECTOR_FLOAT32)
		@Array(length = 3)
		private float[] vFloat32;

		@Column(name = "v_float64")
		@JdbcTypeCode(SqlTypes.VECTOR_FLOAT64)
		@Array(length = 3)
		private double[] vFloat64;

		public DimensionedVectorEntity() {}

		public DimensionedVectorEntity(Long id, float[] vFloat, float[] vFloat32, double[] vFloat64) {
			this.id = id;
			this.vFloat = vFloat;
			this.vFloat32 = vFloat32;
			this.vFloat64 = vFloat64;
		}

		public Long getId() { return id; }
		public float[] getVFloat() { return vFloat; }
		public float[] getVFloat32() { return vFloat32; }
		public double[] getVFloat64() { return vFloat64; }
	}

	@Entity(name = "UnconstrainedVectorEntity")
	public static class UnconstrainedVectorEntity {
		@Id
		private Long id;

		@Column(name = "v_unconstrained")
		@JdbcTypeCode(SqlTypes.VECTOR)
		private float[] vUnconstrained;

		@Column(name = "v_unconstrained_double")
		@JdbcTypeCode(SqlTypes.VECTOR_FLOAT64)
		private double[] vUnconstrainedDouble;

		public UnconstrainedVectorEntity() {}

		public UnconstrainedVectorEntity(Long id, float[] vUnconstrained, double[] vUnconstrainedDouble) {
			this.id = id;
			this.vUnconstrained = vUnconstrained;
			this.vUnconstrainedDouble = vUnconstrainedDouble;
		}

		public Long getId() { return id; }
		public float[] getVUnconstrained() { return vUnconstrained; }
		public double[] getVUnconstrainedDouble() { return vUnconstrainedDouble; }
	}

	@Entity(name = "BoxedVectorEntity")
	public static class BoxedVectorEntity {
		@Id
		private Long id;

		@Column(name = "v_boxed_float")
		@JdbcTypeCode(SqlTypes.VECTOR)
		@Array(length = 3)
		private Float[] vBoxedFloat;

		@Column(name = "v_boxed_double")
		@JdbcTypeCode(SqlTypes.VECTOR_FLOAT64)
		@Array(length = 3)
		private Double[] vBoxedDouble;

		public BoxedVectorEntity() {}

		public BoxedVectorEntity(Long id, Float[] vBoxedFloat, Double[] vBoxedDouble) {
			this.id = id;
			this.vBoxedFloat = vBoxedFloat;
			this.vBoxedDouble = vBoxedDouble;
		}

		public Long getId() { return id; }
		public Float[] getVBoxedFloat() { return vBoxedFloat; }
		public Double[] getVBoxedDouble() { return vBoxedDouble; }
	}
}
