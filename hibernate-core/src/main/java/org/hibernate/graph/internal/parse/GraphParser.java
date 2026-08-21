/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.GraphLanguageParser;
import org.hibernate.grammars.graph.GraphLanguageParserBaseVisitor;
import org.hibernate.graph.GraphNode;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphParserEntityNameResolver;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;

import static org.hibernate.graph.internal.GraphParserLogging.PARSING_LOGGER;
import static org.hibernate.internal.util.StringHelper.repeat;

/**
 * Unified access to the Antlr parser for Hibernate's "graph language"
 *
 * @author Steve Ebersole
 */
public class GraphParser extends GraphLanguageParserBaseVisitor<GraphNode<?>> {
	private final GraphParserEntityNameResolver entityNameResolver;

	private final Stack<GraphImplementor<?>> graphStack = new StandardStack<>();
	private final Stack<AttributeNodeImplementor<?,?,?>> attributeNodeStack = new StandardStack<>();
	private final Stack<SubGraphGenerator> graphSourceStack = new StandardStack<>();

	public GraphParser(GraphParserEntityNameResolver entityNameResolver) {
		this.entityNameResolver = entityNameResolver;
	}

	/**
	 * @apiNote It is important that this form only be used after the session-factory is fully
	 * initialized, especially the {@linkplain SessionFactoryImplementor#getJpaMetamodel()} JPA metamodel}.
	 *
	 * @see GraphParser#GraphParser(GraphParserEntityNameResolver)
	 */
	public GraphParser(SessionFactoryImplementor sessionFactory) {
		this( sessionFactory.getJpaMetamodel()::findEntityType );
	}

	public Stack<GraphImplementor<?>> getGraphStack() {
		return graphStack;
	}

	@Override
	public AttributeNodeImplementor<?,?,?> visitAttributeNode(GraphLanguageParser.AttributeNodeContext attributeNodeContext) {
		final var attributePathContext = attributeNodeContext.attributePath();
		final var attributeQualifierContext = attributePathContext.attributeQualifier();

		final String attributeName = attributePathContext.ATTR_NAME().getText();

		final var subGraphCreator = subGraphCreator( attributeQualifierContext, attributeName );

		final var attributeNode = resolveAttributeNode( attributeName );

		if ( attributeNodeContext.subGraph() != null ) {
			attributeNodeStack.push( attributeNode );
			graphSourceStack.push( subGraphCreator );

			try {
				visitSubGraph( attributeNodeContext.subGraph() );

			}
			finally {
				graphSourceStack.pop();
				attributeNodeStack.pop();
			}
		}

		if ( PARSING_LOGGER.isTraceEnabled() ) {
			PARSING_LOGGER.tracef(
					"%s Finished attribute : %s",
					repeat( "<<", attributeNodeStack.depth() + 1 ),
					attributeName
			);
		}

		return attributeNode;
	}

	private SubGraphGenerator subGraphCreator(GraphLanguageParser.AttributeQualifierContext attributeQualifierContext, String attributeName) {
		if ( attributeQualifierContext == null ) {
			if ( PARSING_LOGGER.isTraceEnabled() ) {
				PARSING_LOGGER.tracef(
						"%s Start attribute : %s",
						repeat( ">>", attributeNodeStack.depth() + 1 ),
						attributeName
				);
			}
			return PathQualifierType.VALUE.getSubGraphCreator();
		}
		else {
			final String qualifierName = attributeQualifierContext.ATTR_NAME().getText();
			if ( PARSING_LOGGER.isTraceEnabled() ) {
				PARSING_LOGGER.tracef(
						"%s Start qualified attribute : %s.%s",
						repeat( ">>", attributeNodeStack.depth() + 1 ),
						attributeName,
						qualifierName
				);
			}
			return resolvePathQualifier( qualifierName ).getSubGraphCreator();
		}
	}

	private AttributeNodeImplementor<?,?,?> resolveAttributeNode(String attributeName) {
		final var currentGraph = graphStack.getCurrent();
		assert currentGraph != null;

		final var attributeNode = currentGraph.findOrCreateAttributeNode( attributeName );
		assert attributeNode != null;

		return attributeNode;
	}

	private PathQualifierType resolvePathQualifier(String qualifier) {
		if ( "key".equalsIgnoreCase( qualifier ) ) {
			return PathQualifierType.KEY;
		}

		if ( "value".equalsIgnoreCase( qualifier ) ) {
			return PathQualifierType.VALUE;
		}

		throw new InvalidGraphException( "Invalid path qualifier [" + qualifier + "] - expecting 'key' or 'value'" );
	}

	@Override
	public SubGraphImplementor<?> visitSubGraph(GraphLanguageParser.SubGraphContext subGraphContext) {
		final String subTypeName =
				subGraphContext.typeIndicator() == null ? null
						: subGraphContext.typeIndicator().TYPE_NAME().getText();

		if ( PARSING_LOGGER.isTraceEnabled() ) {
			PARSING_LOGGER.tracef(
					"%s Starting graph: %s",
					repeat( ">>", attributeNodeStack.depth() + 2 ),
					subTypeName
			);
		}

		final var attributeNode = attributeNodeStack.getCurrent();
		final var subGraphCreator = graphSourceStack.getCurrent();

		final var subGraph = subGraphCreator.createSubGraph(
				attributeNode,
				subTypeName,
				entityNameResolver
		);

		graphStack.push( subGraph );

		try {
			subGraphContext.attributeList().accept( this );
		}
		finally {
			graphStack.pop();
		}

		if ( PARSING_LOGGER.isTraceEnabled() ) {
			PARSING_LOGGER.tracef(
					"%s Finished graph : %s",
					repeat( "<<", attributeNodeStack.depth() + 2 ),
					subGraph.getGraphedType().getTypeName()
			);
		}

		return subGraph;
	}
}
