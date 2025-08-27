/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;

import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.model.source.internal.annotations.AdditionalManagedResourcesImpl;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;
import org.hibernate.models.spi.ModelsContext;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.SourceModelTestHelper.createBuildingContext;

@SuppressWarnings("JUnitMalformedDeclaration")
public class NamedEntityGraphTest {
	@Test
	@ServiceRegistry
	void testNamedEntityGraph(ServiceRegistryScope registryScope) {
		final ManagedResources managedResources = new AdditionalManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-named-entity-graph.xml" )
				.build();
		final ModelsContext ModelsContext = createBuildingContext( managedResources, registryScope.getRegistry() );
		final ClassDetailsRegistry classDetailsRegistry = ModelsContext.getClassDetailsRegistry();

		{
			final ClassDetails employeeClassDetails = classDetailsRegistry.getClassDetails( "Employee" );
			final NamedEntityGraph namedEntityGraph = employeeClassDetails.getAnnotationUsage( NamedEntityGraph.class, ModelsContext );
			assertThat( namedEntityGraph ).isNotNull();

			assertThat( namedEntityGraph.name() ).isEqualTo( "employee" );
			assertThat( namedEntityGraph.includeAllAttributes() ).isTrue();

			final NamedAttributeNode[] attributeNodes = namedEntityGraph.attributeNodes();
			assertThat( attributeNodes ).hasSize( 2 );

			final NamedAttributeNode firstAttributeNode = attributeNodes[0];
			checkAttributeNode( firstAttributeNode, "name", "", "" );

			final NamedAttributeNode secondAttributeNode = attributeNodes[1];
			checkAttributeNode( secondAttributeNode, "address", "employee.address", "" );


			final NamedSubgraph[] subgraphs = namedEntityGraph.subgraphs();
			assertThat( subgraphs ).hasSize( 2 );

			final NamedSubgraph firstSubgraph = subgraphs[0];
			assertThat( firstSubgraph.name() ).isEqualTo( "first.subgraph" );
			assertThat( firstSubgraph.type() ).isEqualTo( void.class );

			final NamedAttributeNode[] firstSubgraphAttributeNodes = firstSubgraph.attributeNodes();
			assertThat( firstSubgraphAttributeNodes ).hasSize( 1 );
			checkAttributeNode( firstSubgraphAttributeNodes[0], "city", "", "" );

			final NamedSubgraph secondSubgraph = subgraphs[1];
			assertThat( secondSubgraph.name() ).isEqualTo( "second.subgraph" );
			assertThat( secondSubgraph.type() ).isEqualTo( String.class );

			final NamedAttributeNode[] secondSubgraphAttributeNodes = secondSubgraph.attributeNodes();
			assertThat( secondSubgraphAttributeNodes ).hasSize( 3 );

			checkAttributeNode( secondSubgraphAttributeNodes[0], "city", "sub1", "" );
			checkAttributeNode( secondSubgraphAttributeNodes[1], "name", "sub", "" );
			checkAttributeNode( secondSubgraphAttributeNodes[2], "surname", "", "" );


			final NamedSubgraph[] subClassSubgraphUsages = namedEntityGraph.subclassSubgraphs();
			assertThat( subClassSubgraphUsages ).isEmpty();
		}

		{
			final ClassDetails addressClassDetails = classDetailsRegistry.getClassDetails( "Address" );
			final NamedEntityGraph namedEntityGraph = addressClassDetails.getDirectAnnotationUsage( NamedEntityGraph.class );
			assertThat( namedEntityGraph ).isNull();
		}
	}

	private static void checkAttributeNode(
			NamedAttributeNode firstAttributeNode,
			String expectedValueName,
			String expectedSubgraph,
			String expectedKeySubgraph) {
		assertThat( firstAttributeNode.value() ).isEqualTo( expectedValueName );
		assertThat( firstAttributeNode.subgraph() ).isEqualTo( expectedSubgraph );
		assertThat( firstAttributeNode.keySubgraph() ).isEqualTo( expectedKeySubgraph );
	}
}
