/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedAttributeNodeImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedEntityGraphImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedSubgraphImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.AnnotationUsage;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MutableAnnotationUsage;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.NamedSubgraph;

import static org.hibernate.boot.models.JpaAnnotations.NAMED_ATTRIBUTE_NODE;
import static org.hibernate.boot.models.internal.AnnotationUsageHelper.applyStringAttributeIfSpecified;
import static org.hibernate.boot.models.xml.internal.XmlProcessingHelper.applyAttributeIfSpecified;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;

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

		final SourceModelBuildingContext modelBuildingContext = xmlDocumentContext.getModelBuildingContext();
		final MutableAnnotationUsage<NamedEntityGraphs> entityGraphsUsage = classDetails.replaceAnnotationUsage(
				JpaAnnotations.NAMED_ENTITY_GRAPH,
				JpaAnnotations.NAMED_ENTITY_GRAPHS,
				modelBuildingContext
		);

		final ArrayList<Object> entityGraphList = arrayList( jaxbEntityGraphs.size() );
		entityGraphsUsage.setAttributeValue( "value", entityGraphList );

		for ( JaxbNamedEntityGraphImpl namedEntityGraph : jaxbEntityGraphs ) {
			final AnnotationUsage<NamedEntityGraph> graphUsage = extractGraph( namedEntityGraph, classDetails, modelBuildingContext, xmlDocumentContext );
			entityGraphList.add( graphUsage );
		}
	}

	private static AnnotationUsage<NamedEntityGraph> extractGraph(
			JaxbNamedEntityGraphImpl jaxbEntityGraph,
			ClassDetails classDetails,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		final MutableAnnotationUsage<NamedEntityGraph> graphUsage = JpaAnnotations.NAMED_ENTITY_GRAPH.createUsage( modelBuildingContext );

		applyStringAttributeIfSpecified( "name", jaxbEntityGraph.getName(), graphUsage );
		applyAttributeIfSpecified( "includeAllAttributes", jaxbEntityGraph.isIncludeAllAttributes(), graphUsage );

		if ( CollectionHelper.isNotEmpty( jaxbEntityGraph.getNamedAttributeNode() ) ) {
			final List<MutableAnnotationUsage<NamedAttributeNode>> attributeNodeList = extractAttributeNodes(
					jaxbEntityGraph.getNamedAttributeNode(),
					classDetails,
					modelBuildingContext,
					xmlDocumentContext
			);
			graphUsage.setAttributeValue( "attributeNodes", attributeNodeList );
		}

		if ( CollectionHelper.isNotEmpty( jaxbEntityGraph.getSubgraph() ) ) {
			final List<MutableAnnotationUsage<NamedSubgraph>> subgraphList = extractSubgraphNodes(
					jaxbEntityGraph.getSubgraph(),
					classDetails,
					modelBuildingContext,
					xmlDocumentContext
			);
			graphUsage.setAttributeValue( "subgraphs", subgraphList );
		}

		if ( CollectionHelper.isNotEmpty( jaxbEntityGraph.getSubclassSubgraph() ) ) {
			final List<MutableAnnotationUsage<NamedSubgraph>> subgraphList = extractSubgraphNodes(
					jaxbEntityGraph.getSubclassSubgraph(),
					classDetails,
					modelBuildingContext,
					xmlDocumentContext
			);
			graphUsage.setAttributeValue( "subclassSubgraphs", subgraphList );
		}

		return graphUsage;
	}

	private static List<MutableAnnotationUsage<NamedAttributeNode>> extractAttributeNodes(
			List<JaxbNamedAttributeNodeImpl> jaxbAttributeNodes,
			ClassDetails classDetails,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbAttributeNodes );

		final ArrayList<MutableAnnotationUsage<NamedAttributeNode>> attributeNodeList = arrayList( jaxbAttributeNodes.size() );
		for ( JaxbNamedAttributeNodeImpl jaxbAttributeNode : jaxbAttributeNodes ) {
			final MutableAnnotationUsage<NamedAttributeNode> namedAttributeNodeAnn = NAMED_ATTRIBUTE_NODE.createUsage( modelBuildingContext );
			attributeNodeList.add( namedAttributeNodeAnn );

			namedAttributeNodeAnn.setAttributeValue( "value", jaxbAttributeNode.getName() );
			applyStringAttributeIfSpecified( "subgraph", jaxbAttributeNode.getSubgraph(), namedAttributeNodeAnn );
			applyStringAttributeIfSpecified( "keySubgraph", jaxbAttributeNode.getKeySubgraph(), namedAttributeNodeAnn );
		}

		return attributeNodeList;
	}

	private static List<MutableAnnotationUsage<NamedSubgraph>> extractSubgraphNodes(
			List<JaxbNamedSubgraphImpl> jaxbSubgraphs,
			ClassDetails classDetails,
			SourceModelBuildingContext modelBuildingContext,
			XmlDocumentContext xmlDocumentContext) {
		assert CollectionHelper.isNotEmpty( jaxbSubgraphs );

		final List<MutableAnnotationUsage<NamedSubgraph>> subgraphAnnotations = arrayList( jaxbSubgraphs.size() );
		for ( JaxbNamedSubgraphImpl jaxbSubgraph : jaxbSubgraphs ) {
			final MutableAnnotationUsage<NamedSubgraph> namedSubGraphUsage = JpaAnnotations.NAMED_SUB_GRAPH.createUsage( modelBuildingContext );
			subgraphAnnotations.add( namedSubGraphUsage );

			namedSubGraphUsage.setAttributeValue( "name", jaxbSubgraph.getName() );

			final ClassDetails typeDetails;
			if ( jaxbSubgraph.getClazz() == null ) {
				typeDetails = ClassDetails.VOID_CLASS_DETAILS;
			}
			else {
				typeDetails = xmlDocumentContext.resolveJavaType( jaxbSubgraph.getClazz() );
			}
			namedSubGraphUsage.setAttributeValue( "type", typeDetails );

			if ( CollectionHelper.isNotEmpty( jaxbSubgraph.getNamedAttributeNode() ) ) {
				namedSubGraphUsage.setAttributeValue(
						"attributeNodes",
						extractAttributeNodes(
								jaxbSubgraph.getNamedAttributeNode(),
								classDetails,
								modelBuildingContext,
								xmlDocumentContext
						)
				);
			}
		}

		return subgraphAnnotations;
	}

}
