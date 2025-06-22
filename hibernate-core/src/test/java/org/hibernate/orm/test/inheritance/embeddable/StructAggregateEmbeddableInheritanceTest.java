/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.embeddable;

import java.util.Set;

import org.hibernate.annotations.Struct;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureParameter;

import org.hibernate.testing.jdbc.SharedDriverManagerTypeCacheClearingIntegrator;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@BootstrapServiceRegistry(
		javaServices = @BootstrapServiceRegistry.JavaService(
				role = AdditionalMappingContributor.class,
				impl = StructAggregateEmbeddableInheritanceTest.class
		),
		// Clear the type cache, otherwise we might run into ORA-21700: object does not exist or is marked for delete
		integrators = SharedDriverManagerTypeCacheClearingIntegrator.class
)
@DomainModel( annotatedClasses = {
		StructAggregateEmbeddableInheritanceTest.TestEntity.class,
		ParentEmbeddable.class,
		ChildOneEmbeddable.class,
		SubChildOneEmbeddable.class,
		ChildTwoEmbeddable.class,
} )
@SessionFactory
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsStructAggregate.class )
public class StructAggregateEmbeddableInheritanceTest implements AdditionalMappingContributor {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_1" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildOneEmbeddable.class );
			assertThat( ( (ChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 1 );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_3" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ParentEmbeddable.class );
		} );
	}

	@Test
	public void testQueryEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where id = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result.getEmbeddable() ).getChildTwoProp() ).isEqualTo( 2L );
		} );
	}

	@Test
	public void testQueryEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEmbeddable result = session.createQuery(
					"select embeddable from TestEntity where id = 4",
					ParentEmbeddable.class
			).getSingleResult();
			assertThat( result.getParentProp() ).isEqualTo( "embeddable_4" );
			assertThat( result ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result ).getChildOneProp() ).isEqualTo( 4 );
			assertThat( ( (SubChildOneEmbeddable) result ).getSubChildOneProp() ).isEqualTo( 4.0 );
		} );
	}

	@Test
	public void testQueryJoinedEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEmbeddable result = session.createQuery(
					"select e from TestEntity t join t.embeddable e where t.id = 2",
					ParentEmbeddable.class
			).getSingleResult();
			assertThat( result.getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result ).getChildTwoProp() ).isEqualTo( 2L );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_5" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildOneEmbeddable.class );
			assertThat( ( (ChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 5 );
			// update values
			result.getEmbeddable().setParentProp( "embeddable_5_new" );
			( (ChildOneEmbeddable) result.getEmbeddable() ).setChildOneProp( 55 );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_5_new" );
			assertThat( ( (ChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 55 );
			result.setEmbeddable( new SubChildOneEmbeddable( "embeddable_6", 6, 6.0 ) );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 5L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_6" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 6 );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getSubChildOneProp() ).isEqualTo( 6.0 );
		} );
	}

	@Test
	public void testFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ProcedureCall structFunction = session.createStoredProcedureCall( "structFunction" )
					.markAsFunctionCall( ParentEmbeddable.class );
			//noinspection unchecked
			final ParentEmbeddable result = (ParentEmbeddable) structFunction.getSingleResult();
			assertThat( result.getParentProp() ).isEqualTo( "function_embeddable" );
			assertThat( result ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result ).getChildOneProp() ).isEqualTo( 1 );
			assertThat( ( (SubChildOneEmbeddable) result ).getSubChildOneProp() ).isEqualTo( 1.0 );
		} );
	}

	@Test
	@SkipForDialect( dialectClass = PostgreSQLDialect.class, majorVersion = 10, reason = "Procedures were only introduced in version 11" )
	@SkipForDialect( dialectClass = PostgresPlusDialect.class, majorVersion = 10, reason = "Procedures were only introduced in version 11" )
	@SkipForDialect( dialectClass = DB2Dialect.class, reason = "DB2 does not support struct types in procedures" )
	public void testProcedure(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Dialect dialect = session.getJdbcServices().getDialect();
			final ParameterMode parameterMode;
			if ( dialect instanceof PostgreSQLDialect ) {
				parameterMode = ParameterMode.INOUT;
			}
			else {
				parameterMode = ParameterMode.OUT;
			}
			final ProcedureCall structFunction = session.createStoredProcedureCall( "structProcedure" );
			final ProcedureParameter<ParentEmbeddable> resultParameter = structFunction.registerParameter(
					"result",
					ParentEmbeddable.class,
					parameterMode
			);
			structFunction.setParameter( resultParameter, null );
			final ParentEmbeddable result = structFunction.getOutputs().getOutputParameterValue( resultParameter );
			assertThat( result ).isInstanceOf( ParentEmbeddable.class );
			assertThat( result.getParentProp() ).isEqualTo( "procedure_embeddable" );
			assertThat( result ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result ).getChildTwoProp() ).isEqualTo( 2 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildOneEmbeddable( "embeddable_1", 1 ) ) );
			session.persist( new TestEntity( 2L, new ChildTwoEmbeddable( "embeddable_2", 2L ) ) );
			session.persist( new TestEntity( 3L, new ParentEmbeddable( "embeddable_3" ) ) );
			session.persist( new TestEntity( 4L, new SubChildOneEmbeddable( "embeddable_4", 4, 4.0 ) ) );
			session.persist( new TestEntity( 5L, new ChildOneEmbeddable( "embeddable_5", 5 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Override
	public void contribute(
			AdditionalMappingContributions contributions,
			InFlightMetadataCollector metadata,
			ResourceStreamLocator resourceStreamLocator,
			MetadataBuildingContext buildingContext) {
		final Namespace namespace = new Namespace(
				PhysicalNamingStrategyStandardImpl.INSTANCE,
				null,
				new Namespace.Name( null, null )
		);

		//---------------------------------------------------------
		// PostgreSQL
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgreSQL structFunction",
						namespace,
						"create function structFunction() returns inheritance_embeddable as $$ declare result inheritance_embeddable; begin result.parentProp = 'function_embeddable'; result.childOneProp = 1; result.subChildOneProp = 1.0; result.childTwoProp = null; result.embeddable_type = 'sub_child_one'; return result; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgreSQL structProcedure",
						namespace,
						"create procedure structProcedure(INOUT result inheritance_embeddable) AS $$ declare res inheritance_embeddable; begin res.parentProp = 'procedure_embeddable'; res.childOneProp = null; res.subChildOneProp = null; res.childTwoProp = 2; res.embeddable_type = 'ChildTwoEmbeddable'; result = res; end $$ language plpgsql",
						"drop procedure structProcedure",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// PostgresPlus
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structFunction",
						namespace,
						"create function structFunction() returns inheritance_embeddable as $$ declare result inheritance_embeddable; begin result.parentProp = 'function_embeddable'; result.childOneProp = 1; result.subChildOneProp = 1.0; result.childTwoProp = null; result.embeddable_type = 'sub_child_one'; return result; end $$ language plpgsql",
						"drop function structFunction",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgrePlus structProcedure",
						namespace,
						"create procedure structProcedure(result INOUT inheritance_embeddable) AS $$ declare res inheritance_embeddable; begin res.parentProp = 'procedure_embeddable'; res.childOneProp = null; res.subChildOneProp = null; res.childTwoProp = 2; res.embeddable_type = 'ChildTwoEmbeddable'; result = res; end $$ language plpgsql",
						"drop procedure structProcedure",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// DB2
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"DB2 structFunction",
						namespace,
						"create function structFunction() returns inheritance_embeddable language sql RETURN select inheritance_embeddable()..parentProp('function_embeddable')..childOneProp(1)..subChildOneProp(1.0)..embeddable_type('sub_child_one') from (values (1)) t",
						"drop function structFunction",
						Set.of( DB2Dialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// Oracle
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle structFunction",
						namespace,
						"create function structFunction return inheritance_embeddable is result inheritance_embeddable; begin " +
								"result := inheritance_embeddable(" +
								"parentProp => 'function_embeddable'," +
								"childOneProp => 1," +
								"subChildOneProp => 1.0," +
								"childTwoProp => null," +
								"embeddable_type => 'sub_child_one'" +
								"); return result; end;",
						"drop function structFunction",
						Set.of( OracleDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle structProcedure",
						namespace,
						"create procedure structProcedure(result OUT inheritance_embeddable) AS begin " +
								"result := inheritance_embeddable(" +
								"parentProp => 'procedure_embeddable'," +
								"childOneProp => null," +
								"subChildOneProp => null," +
								"childTwoProp => 2," +
								"embeddable_type => 'ChildTwoEmbeddable'" +
								"); end;",
						"drop procedure structProcedure",
						Set.of( OracleDialect.class.getName() )
				)
		);
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		@Struct( name = "inheritance_embeddable" )
		private ParentEmbeddable embeddable;

		public TestEntity() {
		}

		public TestEntity(Long id, ParentEmbeddable embeddable) {
			this.id = id;
			this.embeddable = embeddable;
		}

		public ParentEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}
	}
}
