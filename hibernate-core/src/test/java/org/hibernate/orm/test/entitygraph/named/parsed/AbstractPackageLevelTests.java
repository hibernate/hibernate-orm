/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.parsed;

import org.hibernate.DuplicateMappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.orm.test.entitygraph.named.parsed.pckgwithgraphnameduplication.Duplicator;

import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.hibernate.orm.test.entitygraph.parser.AssertionHelper.assertBasicAttributes;


/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public abstract class AbstractPackageLevelTests {
	protected static void assertBasicGraph(SessionFactoryImplementor sessionFactory, String name, String... names) {
		RootGraphImplementor<?> graph = sessionFactory.findEntityGraphByName( name );
		assertThat( graph.getName() ).isEqualTo( name );
		assertBasicAttributes( graph, names );
	}

	@Test
	void testDuplication(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry serviceRegistry = registryScope.getRegistry();
		try {
			new MetadataSources( serviceRegistry )
					.addAnnotatedClass( Duplicator.class )
					.addPackage( "org.hibernate.orm.test.entitygraph.named.parsed.pckgwithgraphnameduplication" )
					.buildMetadata();
			fail( "Expected an exception" );
		}
		catch (DuplicateMappingException expected) {
		}
	}
}
