/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.srf;

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
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
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
@SkipForDialect(dialectClass = PostgresPlusDialect.class)
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
	}

	@Override
	public void contributeFunctions(FunctionContributions functionContributions) {
		functionContributions.getFunctionRegistry().namedSetReturningDescriptorBuilder(
				"mySrf",
				SetReturningFunctionTypeResolver.builder()
						.invariant( "val", StandardBasicTypes.INTEGER, "v2" )
						.build()
		).register();
	}

	@Test
	public void testUnnest(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-set-returning-function-custom-example[]
			Integer singleResult = em.createQuery(
							"select e.val from mySrf(1) e",
							Integer.class
					)
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

}
