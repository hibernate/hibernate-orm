/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.grammars.graph.ModernGraphLanguageParser;
import org.hibernate.grammars.graph.ModernGraphLanguageParserBaseVisitor;
import org.hibernate.graph.GraphNode;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;

import static org.hibernate.graph.internal.GraphParserLogging.PARSING_LOGGER;

/**
 * Unified access to the Antlr parser for Hibernate's "modern graph language"
 *
 */
public class ModernGraphParser extends ModernGraphLanguageParserBaseVisitor<GraphNode<?>> {
	private final EntityNameResolver entityNameResolver;

	private final Stack<GraphImplementor<?>> graphStack = new StandardStack<>();
	private final Stack<AttributeNodeImplementor<?, ?, ?>> attributeNodeStack = new StandardStack<>();
	private final Stack<SubGraphGenerator> graphSourceStack = new StandardStack<>();

	public ModernGraphParser(EntityNameResolver entityNameResolver) {
		this.entityNameResolver = entityNameResolver;
	}

	/**
	 * @apiNote It is important that this form only be used after the session-factory is fully
	 * initialized, especially the {@linkplain SessionFactoryImplementor#getJpaMetamodel()} JPA metamodel}.
	 * @see ModernGraphParser#ModernGraphParser(EntityNameResolver)
	 */
	public ModernGraphParser(SessionFactoryImplementor sessionFactory) {
		this( new EntityNameResolverSessionFactory( sessionFactory ) );
	}

	public Stack<GraphImplementor<?>> getGraphStack() {
		return graphStack;
	}

	@Override
	public GraphNode<?> visitSubGraph(ModernGraphLanguageParser.SubGraphContext subGraphContext) {
		final String subTypeName = subGraphContext.subTypeIndicator() == null ?
				null :
				subGraphContext.subTypeIndicator().TYPE_NAME().getText();

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Starting subtype graph : %s",
					StringHelper.repeat( ">>", attributeNodeStack.depth() + 2 ),
					subTypeName
			);
		}

		final AttributeNodeImplementor<?, ?, ?> attributeNode = attributeNodeStack.getCurrent();

		SubGraphImplementor<?> subGraph = createSubGraph( attributeNode, subTypeName );

		graphStack.push( subGraph );


		try {
			subGraphContext.attributeList().accept( this );
		}
		finally {
			graphStack.pop();
		}

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Finished subtype graph : %s",
					StringHelper.repeat( "<<", attributeNodeStack.depth() + 2 ),
					subGraph.getGraphedType().getTypeName()
			);
		}

		return subGraph;
	}

	private SubGraphImplementor<?> createSubGraph(AttributeNodeImplementor<?, ?, ?> attributeNode, String subTypeName) {
		SubGraphImplementor<?> subGraph;

		var shouldCreateTreatedSubgraph = attributeNode == null && subTypeName != null;

		if ( shouldCreateTreatedSubgraph ) {
			var currentGraph = graphStack.getCurrent();

			subGraph = currentGraph.addTreatedSubgraph(
					entityNameResolver.resolveEntityName( subTypeName )
			);

		}
		else {
			final SubGraphGenerator subGraphCreator = graphSourceStack.getCurrent();

			subGraph = subGraphCreator.createSubGraph(
					attributeNode,
					subTypeName,
					entityNameResolver
			);
		}

		return subGraph;
	}

	@Override
	public AttributeNodeImplementor<?, ?, ?> visitAttributeNode(ModernGraphLanguageParser.AttributeNodeContext attributeNodeContext) {
		final String attributeName = attributeNodeContext.attributePath().ATTR_NAME().getText();

		final SubGraphGenerator subGraphCreator;

		if ( attributeNodeContext.attributePath().attributeQualifier() == null ) {
			if ( PARSING_LOGGER.isTraceEnabled() ) {
				PARSING_LOGGER.tracef(
						"%s Start attribute : %s",
						StringHelper.repeat( ">>", attributeNodeStack.depth() + 1 ),
						attributeName
				);
			}

			subGraphCreator = PathQualifierType.VALUE.getSubGraphCreator();
		}
		else {
			final String qualifierName = attributeNodeContext.attributePath()
					.attributeQualifier()
					.ATTR_NAME()
					.getText();

			if ( PARSING_LOGGER.isTraceEnabled() ) {
				PARSING_LOGGER.tracef(
						"%s Start qualified attribute : %s.%s",
						StringHelper.repeat( ">>", attributeNodeStack.depth() + 1 ),
						attributeName,
						qualifierName
				);
			}

			final PathQualifierType pathQualifierType = resolvePathQualifier( qualifierName );
			subGraphCreator = pathQualifierType.getSubGraphCreator();
		}

		final AttributeNodeImplementor<?, ?, ?> attributeNode = resolveAttributeNode( attributeName );

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
					StringHelper.repeat( "<<", attributeNodeStack.depth() + 1 ),
					attributeName
			);
		}

		return attributeNode;
	}

	private AttributeNodeImplementor<?, ?, ?> resolveAttributeNode(String attributeName) {
		final GraphImplementor<?> currentGraph = graphStack.getCurrent();
		assert currentGraph != null;

		final AttributeNodeImplementor<?, ?, ?> attributeNode = currentGraph.findOrCreateAttributeNode( attributeName );
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
}
