/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.array;

import java.util.List;

import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.type.OracleArrayJdbcType;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.ArrayJavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = AdditionalMappingContributor.class,
				impl = ArrayAggregateTest.UdtContributor.class
		),
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStructuralArrays.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsArrayAgg.class)
@SkipForDialect(dialectClass = SpannerDialect.class, reason = "Doesn't support array_agg ordering yet")
public class ArrayAggregateTest {

	public static class UdtContributor implements AdditionalMappingContributor {
		@Override
		public void contribute(
				AdditionalMappingContributions contributions,
				InFlightMetadataCollector metadata,
				ResourceStreamLocator resourceStreamLocator,
				MetadataBuildingContext buildingContext) {
			if ( metadata.getDatabase().getDialect() instanceof OracleDialect ) {
				final TypeConfiguration typeConfiguration = metadata.getTypeConfiguration();
				final JavaTypeRegistry javaTypeRegistry = typeConfiguration.getJavaTypeRegistry();
				final JdbcTypeRegistry jdbcTypeRegistry = typeConfiguration.getJdbcTypeRegistry();
				new OracleArrayJdbcType(
						jdbcTypeRegistry.getDescriptor( SqlTypes.VARCHAR ),
						"StringArray"
				).addAuxiliaryDatabaseObjects(
						new ArrayJavaType<>( javaTypeRegistry.getDescriptor( String.class ) ),
						null,
						Size.nil(),
						metadata.getDatabase(),
						typeConfiguration.getCurrentBaseSqlTypeIndicators()
				);
			}
		}
	}

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			final EntityOfBasics e1 = new EntityOfBasics( 1 );
			e1.setTheString( "abc" );
			final EntityOfBasics e2 = new EntityOfBasics( 2 );
			e2.setTheString( "def" );
			final EntityOfBasics e3 = new EntityOfBasics( 3 );
			em.persist( e1 );
			em.persist( e2 );
			em.persist( e3 );
		} );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEmpty(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-array-agg-example[]
			List<String[]> results = em.createQuery( "select array_agg(e.data) within group (order by e.id) from BasicEntity e", String[].class )
					.getResultList();
			//end::hql-array-agg-example[]
			assertEquals( 1, results.size() );
			assertNull( results.get( 0 ) );
		} );
	}

	@Test
	public void testWithoutNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery( "select array_agg(e.theString) within group (order by e.theString) from EntityOfBasics e where e.theString is not null", String[].class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertArrayEquals( new String[]{ "abc", "def" }, results.get( 0 ) );
		} );
	}

	@Test
	public void testWithNull(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<String[]> results = em.createQuery( "select array_agg(e.theString) within group (order by e.theString asc nulls last) from EntityOfBasics e", String[].class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertArrayEquals( new String[]{ "abc", "def", null }, results.get( 0 ) );
		} );
	}

	@Test
	public void testCompareAgainstArray(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Integer> results = em.createQuery( "select 1 where array('abc','def',null) is not distinct from (select array_agg(e.theString) within group (order by e.theString asc nulls last) from EntityOfBasics e)", Integer.class )
					.getResultList();
			assertEquals( 1, results.size() );
		} );
	}

	@Test
	public void testNodeBuilder(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<String[]> cq = cb.createQuery( String[].class );
			final JpaRoot<EntityOfBasics> root = cq.from( EntityOfBasics.class );
			cq.select( cb.arrayAgg( cb.asc( root.get( "theString" ), false ), root.get( "theString" ) ) );
			List<String[]> results = em.createQuery( cq ).getResultList();
			assertEquals( 1, results.size() );
			assertArrayEquals( new String[]{ "abc", "def", null }, results.get( 0 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19666")
	public void testNonExistingArrayType(SessionFactoryScope scope) {
		scope.inSession( em -> {
			List<Integer[]> results = em.createQuery( "select array_agg(e.id) within group (order by e.id) from EntityOfBasics e", Integer[].class )
					.getResultList();
			assertEquals( 1, results.size() );
			assertArrayEquals( new Integer[]{ 1, 2, 3 }, results.get( 0 ) );
		} );
	}

}
