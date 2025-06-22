/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.srf;

import jakarta.persistence.Tuple;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.AdditionalMappingContributions;
import org.hibernate.boot.spi.AdditionalMappingContributor;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.StandardBasicTypes;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Christian Beikov
 */
@BootstrapServiceRegistry(
		javaServices = {
				@BootstrapServiceRegistry.JavaService(
						role = AdditionalMappingContributor.class,
						impl = CustomSetReturningFunctionTest.class
				),
				@BootstrapServiceRegistry.JavaService(
						role = FunctionContributor.class,
						impl = CustomSetReturningFunctionTest.class
				)
		}
)
@DomainModel(standardModels = StandardDomainModel.LIBRARY)
@SessionFactory
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(OracleDialect.class)
@RequiresDialect(DB2Dialect.class)
@RequiresDialect(SQLServerDialect.class)
@RequiresDialect(SybaseASEDialect.class)
@RequiresDialect(HANADialect.class)
public class CustomSetReturningFunctionTest implements AdditionalMappingContributor, FunctionContributor {

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
						"PostgreSQL mySrf",
						namespace,
						"create or replace function mySrf(v1 int, v2 OUT int) as $$ begin v2 := v1; end $$ language plpgsql",
						"drop function mySrf",
						Set.of( PostgreSQLDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// PostgresPlus
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"PostgresPlus mySrf",
						namespace,
						"create or replace function mySrf(v1 int, v2 OUT int) as $$ begin v2 := v1; end $$ language plpgsql",
						"drop function mySrf",
						Set.of( PostgresPlusDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// Oracle
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle mySrf object type",
						namespace,
						"create type mySrf_type as object (v2 number)",
						"drop type mySrf_type",
						Set.of( OracleDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle mySrf table type",
						namespace,
						"create type mySrf_ttype is table of mySrf_type",
						"drop type mySrf_ttype",
						Set.of( OracleDialect.class.getName() )
				)
		);
		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Oracle mySrf",
						namespace,
						"create or replace function mySrf(v1 IN number) return mySrf_ttype as ret mySrf_ttype := mySrf_ttype(); begin ret.extend; ret(ret.last) := mySrf_type(v1); return ret; end;",
						"drop function mySrf",
						Set.of( OracleDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// DB2
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"DB2 mySrf",
						namespace,
						"create function mySrf (v1 int) returns table (v2 int) language sql reads sql data no external action deterministic return select v1 from sysibm.dual",
						"drop function mySrf",
						Set.of( DB2Dialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// SQL Server
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"SQL Server mySrf",
						namespace,
						"create function mySrf (@v1 int) returns table as return select @v1 v2",
						"drop function mySrf",
						Set.of( SQLServerDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// Sybase ASE
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"Sybase ASE mySrf",
						namespace,
						"create function mySrf(@v1 int) returns table (v2 int) as return select @v1",
						"drop function mySrf",
						Set.of( SybaseASEDialect.class.getName() )
				)
		);

		//---------------------------------------------------------
		// HANA
		//---------------------------------------------------------

		contributions.contributeAuxiliaryDatabaseObject(
				new NamedAuxiliaryDatabaseObject(
						"HANA mySrf",
						namespace,
						"create or replace function mySrf(v1 int) returns table (v2 int) language sqlscript as begin return select :v1 as v2 from sys.dummy; end;",
						"drop function mySrf",
						Set.of( HANADialect.class.getName() )
				)
		);
	}

	//tag::hql-set-returning-function-contributor-example[]
	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		functionContributions.getFunctionRegistry().namedSetReturningDescriptorBuilder(
				"mySrf",
				SetReturningFunctionTypeResolver.builder()
						.invariant( "val", StandardBasicTypes.INTEGER, "v2" )
						.build()
		).register();
	}
	//end::hql-set-returning-function-contributor-example[]

	@Test
	public void testUnnest(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-set-returning-function-custom-example[]
			Integer singleResult = em.createQuery( "select e.val from mySrf(1) e", Integer.class )
					.getSingleResult();
			//end::hql-set-returning-function-custom-example[]

			assertEquals( 1, singleResult );
		} );
	}

	@Test
	public void testNodeBuilderUnnest(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> cq = cb.createQuery(Integer.class);
			final JpaFunctionRoot<Object> root = cq.from( cb.setReturningFunction( "mySrf", cb.literal( 1 ) ) );
			cq.select( root.get( "val" ) );
			Integer singleResult = em.createQuery( cq ).getSingleResult();

			assertEquals( 1, singleResult );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase ASE has no way to emulate ordinality for user defined set-returning functions")
	public void testUnnestOrdinality(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-set-returning-function-ordinality-custom-example[]
			Tuple singleResult = em.createQuery(
							"select index(e), e.val from mySrf(1) e",
							Tuple.class
					)
					.getSingleResult();
			//end::hql-set-returning-function-ordinality-custom-example[]

			assertEquals( 1L, singleResult.get( 0 ) );
			assertEquals( 1, singleResult.get( 1 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase ASE has no way to emulate ordinality for user defined set-returning functions")
	public void testNodeBuilderUnnestOrdinality(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final JpaFunctionRoot<Object> root = cq.from( cb.setReturningFunction( "mySrf", cb.literal( 1 ) ) );
			cq.multiselect( root.index(), root.get( "val" ) );
			Tuple singleResult = em.createQuery( cq ).getSingleResult();

			assertEquals( 1L, singleResult.get( 0 ) );
			assertEquals( 1, singleResult.get( 1 ) );
		} );
	}

}
