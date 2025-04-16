/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.xml.internal;

import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedAttributeNodeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedEntityGraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedSubgraphImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.annotations.internal.NamedAttributeNodeJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedEntityGraphJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedEntityGraphsJpaAnnotation;
import org.hibernate.boot.models.annotations.internal.NamedSubgraphJpaAnnotation;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;

import static org.hibernate.boot.models.JpaAnnotations.NAMED_ATTRIBUTE_NODE;
import static org.hibernate.boot.models.JpaAnnotations.NAMED_SUBGRAPH;

/**
 * Processing for JPA entity graphs from XML
 *
 * @author Steve Ebersole
 */
public class EntityGraphProcessing {
	public static void applyEntityGraphs(
			JaxbEntityImpl jaxbEntity,
			MutableClassDetails classDetails,
			XmlDocumentContext xmlDocumentContext) {
		final List<JaxbNamedEntityGraphImpl> jaxbEntityGraphs = jaxbEntity.getNamedEntityGraphs();
		if ( CollectionHelper.isEmpty( jaxbEntityGraphs ) ) {
			return;
		}

		final ModelsContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final NamedEntityGraphsJpaAnnotation entityGraphsUsage = (NamedEntityGraphsJpaAnnotation) classDetails.replaceAnnotationUsage(
				JpaAnnotations.NAMED_ENTITY_GRAPH,
				JpaAnnotations.NAMED_ENTITY_GRAPHS,
				modelBuildingContext
		);

		final NamedEntityGraph[] graphs = new NamedEntityGraph[jaxbEntityGraphs.size()];
		entityGraphsUsage.value( graphs );

		for ( int i = 0; i < jaxbEntityGraphs.size(); i++ ) {
			graphs[i] = extractGraph( jaxbEntityGraphs.get( i ), classDetails, modelBuildingContext, xmlDocumentContext );
		}
	}

	private static NamedEntityGraph extractGraph(
			JaxbNamedEntityGraphImpl jaxbEntityGraph,
			ClassDetails classDetails,
			ModelsContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		final NamedEntityGraphJpaAnnotation graphUsage = JpaAnnotations.NAMED_ENTITY_GRAPH.createUsage( modelBuildingContext );

		if ( StringHelper.isNotEmpty( jaxbEntityGraph.getName() ) ) {
			graphUsage.name( jaxbEntityGraph.getName() );
		}

		if ( jaxbEntityGraph.isIncludeAllAttributes() != null ) {
			graphUsage.includeAllAttributes( jaxbEntityGraph.isIncludeAllAttributes() );
		}

		if ( CollectionHelper.isNotEmpty( jaxbEntityGraph.getNamedAttributeNode() ) ) {
			graphUsage.attributeNodes( extractAttributeNodes(
					jaxbEntityGraph.getNamedAttributeNode(),
					classDetails,
					modelBuildingContext,
					xmlDocumentContext
			) );
		}

		if ( CollectionHelper.isNotEmpty( jaxbEntityGraph.getSubgraph() ) ) {
			graphUsage.subgraphs( extractSubgraphNodes(
					jaxbEntityGraph.getSubgraph(),
					classDetails,
					modelBuildingContext,
					xmlDocumentContext
			) );
		}

		if ( CollectionHelper.isNotEmpty( jaxbEntityGraph.getSubclassSubgraph() ) ) {
			graphUsage.subclassSubgraphs( extractSubgraphNodes(
					jaxbEntityGraph.getSubclassSubgraph(),
					classDetails,
					modelBuildingContext,
					xmlDocumentContext
			) );
		}

		return graphUsage;
	}

	private static NamedAttributeNode[] extractAttributeNodes(
			List<JaxbNamedAttributeNodeImpl> jaxbAttributeNodes,
			ClassDetails classDetails,
			ModelsContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbAttributeNodes );

		final NamedAttributeNode[] attributeNodes = new NamedAttributeNode[jaxbAttributeNodes.size()];
		for ( int i = 0; i < jaxbAttributeNodes.size(); i++ ) {
			final NamedAttributeNodeJpaAnnotation namedAttributeNodeAnn = NAMED_ATTRIBUTE_NODE.createUsage( modelBuildingContext );
			attributeNodes[i] = namedAttributeNodeAnn;

			final JaxbNamedAttributeNodeImpl jaxbAttributeNode = jaxbAttributeNodes.get( i );
			namedAttributeNodeAnn.value( jaxbAttributeNode.getName() );

			if ( StringHelper.isNotEmpty( jaxbAttributeNode.getSubgraph() ) ) {
				namedAttributeNodeAnn.subgraph( jaxbAttributeNode.getSubgraph() );
			}
			if ( StringHelper.isNotEmpty( jaxbAttributeNode.getKeySubgraph() ) ) {
				namedAttributeNodeAnn.keySubgraph( jaxbAttributeNode.getKeySubgraph() );
			}
		}

		return attributeNodes;
	}

	private static NamedSubgraph[] extractSubgraphNodes(
			List<JaxbNamedSubgraphImpl> jaxbSubgraphs,
			ClassDetails classDetails,
			ModelsContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbSubgraphs );

		final NamedSubgraph[] subgraphs = new NamedSubgraph[jaxbSubgraphs.size()];
		for ( int i = 0; i < jaxbSubgraphs.size(); i++ ) {
			final NamedSubgraphJpaAnnotation namedSubGraphUsage = NAMED_SUBGRAPH.createUsage( modelBuildingContext );
			subgraphs[i] = namedSubGraphUsage;

			final JaxbNamedSubgraphImpl jaxbSubgraph = jaxbSubgraphs.get( i );
			namedSubGraphUsage.name( jaxbSubgraph.getName() );

			Class<?> type;
			if ( jaxbSubgraph.getClazz() == null ) {
				type = void.class;
			}
			else {
				final ClassDetails typeDetails = xmlDocumentContext.resolveJavaType( jaxbSubgraph.getClazz() );
				type = typeDetails.toJavaClass();
			}
			namedSubGraphUsage.type( type );

			if ( CollectionHelper.isNotEmpty( jaxbSubgraph.getNamedAttributeNode() ) ) {
				namedSubGraphUsage.attributeNodes( extractAttributeNodes(
						jaxbSubgraph.getNamedAttributeNode(),
						classDetails,
						modelBuildingContext,
						xmlDocumentContext
				) );
			}
		}

		return subgraphs;
	}

}
