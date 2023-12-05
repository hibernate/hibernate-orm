/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.xml.dynamic;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.internal.BootstrapContextImpl;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.model.process.spi.ManagedResources;
import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.orm.test.boot.models.ManagedResourcesImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;

import org.junit.jupiter.api.Test;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.boot.models.categorize.spi.ManagedResourcesProcessor.processManagedResources;

public class NamedEntityGraphTest {
	@Test
	void testNamedEntityGraph() {
		final ManagedResources managedResources = new ManagedResourcesImpl.Builder()
				.addXmlMappings( "mappings/models/dynamic/dynamic-named-entity-graph.xml" )
				.build();
		try (StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().build()) {
			final BootstrapContextImpl bootstrapContext = new BootstrapContextImpl(
					serviceRegistry,
					new MetadataBuilderImpl.MetadataBuildingOptionsImpl( serviceRegistry )
			);
			final CategorizedDomainModel categorizedDomainModel = processManagedResources(
					managedResources,
					bootstrapContext
			);

			final Set<EntityHierarchy> entityHierarchies = categorizedDomainModel.getEntityHierarchies();
			assertThat( entityHierarchies ).hasSize( 1 );

			entityHierarchies.forEach(
					entityHierarchy -> {
						final EntityTypeMetadata root = entityHierarchy.getRoot();
						final String entityName = root.getEntityName();

						final AnnotationUsage<NamedEntityGraph> namedEntityGraphAnnotationUsage = root.getClassDetails()
								.getAnnotationUsage( NamedEntityGraph.class );

						if ( entityName.equals( "Address" ) ) {
							assertThat( namedEntityGraphAnnotationUsage ).isNull();
						}
						else {
							assertThat( namedEntityGraphAnnotationUsage ).isNotNull();

							final String graphName = namedEntityGraphAnnotationUsage.getAttributeValue( "name" );
							assertThat( graphName ).isEqualTo( "employee" );

							final boolean includeAllAttributes = namedEntityGraphAnnotationUsage.getAttributeValue(
									"includeAllAttributes" );
							assertThat( includeAllAttributes ).isTrue();

							List<AnnotationUsage<NamedAttributeNode>> namedAttributeNodeUsage = namedEntityGraphAnnotationUsage
									.getAttributeValue( "attributeNodes" );
							assertThat( namedAttributeNodeUsage ).size().isEqualTo( 2 );

							// check NamedEntityGraph attributeNodes

							AnnotationUsage<NamedAttributeNode> firstAttributeNode = namedAttributeNodeUsage.get( 0 );
							checkAttributeNode( firstAttributeNode, "name", "", "" );

							AnnotationUsage<NamedAttributeNode> secondAttributeNode = namedAttributeNodeUsage.get( 1 );
							checkAttributeNode( secondAttributeNode, "address", "employee.address", "" );

							// check NamedEntityGraph subgraphs
							final List<AnnotationUsage<NamedSubgraph>> subgraphUsages = namedEntityGraphAnnotationUsage
									.getAttributeValue( "subgraphs" );
							assertThat( subgraphUsages ).size().isEqualTo( 2 );

							AnnotationUsage<NamedSubgraph> firstSubgraph = subgraphUsages.get( 0 );
							assertThat( firstSubgraph.getString( "name" ) ).isEqualTo( "first.subgraph" );
							assertThat( firstSubgraph.<ClassDetails>getAttributeValue( "type" ).getName() )
									.isEqualTo( void.class.getName() );

							// check first NamedSubgraph attributeNodes

							namedAttributeNodeUsage = firstSubgraph.getAttributeValue( "attributeNodes" );
							assertThat( namedAttributeNodeUsage ).size().isEqualTo( 1 );

							checkAttributeNode( namedAttributeNodeUsage.get( 0 ), "city", "", "" );

							AnnotationUsage<NamedSubgraph> secondSubgraph = subgraphUsages.get( 1 );
							assertThat( secondSubgraph.getString( "name" ) ).isEqualTo( "second.subgraph" );
							assertThat( secondSubgraph.<ClassDetails>getAttributeValue( "type" ).getName() )
									.isEqualTo( String.class.getName() );

							namedAttributeNodeUsage = secondSubgraph.getAttributeValue( "attributeNodes" );
							assertThat( namedAttributeNodeUsage ).size().isEqualTo( 3 );

							// check second NamedSubgraph attributeNodes
							checkAttributeNode( namedAttributeNodeUsage.get( 0 ), "city", "sub1", "" );
							checkAttributeNode( namedAttributeNodeUsage.get( 1 ), "name", "sub", "" );
							checkAttributeNode( namedAttributeNodeUsage.get( 2 ), "surname", "", "" );


							final List<AnnotationUsage<NamedSubgraph>> subClassSubgraphUsages = namedEntityGraphAnnotationUsage
									.getAttributeValue( "subclassSubgraphs" );
							assertThat( subClassSubgraphUsages ).size().isEqualTo( 0 );

						}
					}
			);
		}
	}

	private static void checkAttributeNode(
			AnnotationUsage<NamedAttributeNode> firstAttributeNode,
			String expectedValueName,
			String expectedSubgraph,
			String expectedKeySubgraph) {
		assertThat( firstAttributeNode.getString( "value" ) ).isEqualTo( expectedValueName );
		assertThat( firstAttributeNode.getString( "subgraph" ) ).isEqualTo( expectedSubgraph );
		assertThat( firstAttributeNode.getString( "keySubgraph" ) ).isEqualTo( expectedKeySubgraph );
	}
}
