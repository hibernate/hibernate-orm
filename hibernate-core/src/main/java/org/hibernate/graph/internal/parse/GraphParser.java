/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.InvalidGraphException;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;

import org.jboss.logging.Logger;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * @author Steve Ebersole
 */
public class GraphParser extends GraphLanguageParserBaseVisitor {
	public static final Logger PARSING_LOGGER = Logger.getLogger( "org.hibernate.orm.graph.parsing" );

	/**
	 * Parse the passed graph textual representation into the passed Graph.
	 */
	public static void parseInto(
			GraphImplementor<?> targetGraph,
			String graphString,
			SessionFactoryImplementor sessionFactory) {
		// Build the lexer
		final GraphLanguageLexer lexer = new GraphLanguageLexer( CharStreams.fromString( graphString ) );

		// Build the parser...
		final GraphLanguageParser parser = new GraphLanguageParser( new CommonTokenStream( lexer ) );

		// Build an instance of this class as a visitor
		final GraphParser visitor = new GraphParser( sessionFactory );
		visitor.graphStack.push( targetGraph );
		try {
			visitor.visitGraph( parser.graph() );
		}
		finally {
			visitor.graphStack.pop();

			assert visitor.graphStack.isEmpty();
		}
	}

	private final SessionFactoryImplementor sessionFactory;

	private final Stack<GraphImplementor<?>> graphStack = new StandardStack<>();
	private final Stack<AttributeNodeImplementor<?>> attributeNodeStack = new StandardStack<>();
	private final Stack<SubGraphGenerator> graphSourceStack = new StandardStack<>();

	public GraphParser(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public AttributeNodeImplementor visitAttributeNode(GraphLanguageParser.AttributeNodeContext ctx) {
		final String attributeName = ctx.attributePath().NAME().getText();

		final SubGraphGenerator subGraphCreator;

		if ( ctx.attributePath().attributeQualifier() == null ) {
			if ( PARSING_LOGGER.isDebugEnabled() ) {
				PARSING_LOGGER.debugf(
						"%s Start attribute : %s",
						StringHelper.repeat( ">>", attributeNodeStack.depth() + 1 ),
						attributeName
				);
			}

			subGraphCreator = PathQualifierType.VALUE.getSubGraphCreator();
		}
		else {
			final String qualifierName = ctx.attributePath().attributeQualifier().NAME().getText();

			if ( PARSING_LOGGER.isDebugEnabled() ) {
				PARSING_LOGGER.debugf(
						"%s Start qualified attribute : %s.%s",
						StringHelper.repeat( ">>", attributeNodeStack.depth() + 1 ),
						attributeName,
						qualifierName
				);
			}

			final PathQualifierType pathQualifierType = resolvePathQualifier( qualifierName );
			subGraphCreator = pathQualifierType.getSubGraphCreator();
		}

		final AttributeNodeImplementor attributeNode = resolveAttributeNode( attributeName );

		if ( ctx.subGraph() != null ) {
			attributeNodeStack.push( attributeNode );
			graphSourceStack.push( subGraphCreator );

			try {
				visitSubGraph( ctx.subGraph() );

			}
			finally {
				graphSourceStack.pop();
				attributeNodeStack.pop();
			}
		}

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Finished attribute : %s",
					StringHelper.repeat( "<<", attributeNodeStack.depth() + 1 ),
					attributeName
			);
		}

		return attributeNode;
	}


	private AttributeNodeImplementor resolveAttributeNode(String attributeName) {
		final GraphImplementor<?> currentGraph = graphStack.getCurrent();
		assert currentGraph != null;

		final AttributeNodeImplementor attributeNode = currentGraph.addAttributeNode( attributeName );
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

		throw new InvalidGraphException( "Invalid path qualifier [" + qualifier + "] - expecting `key` or `value`" );
	}

	@Override
	public SubGraphImplementor visitSubGraph(GraphLanguageParser.SubGraphContext ctx) {
		final String subTypeName = ctx.subType() == null ? null : ctx.subType().getText();

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Starting graph : %s",
					StringHelper.repeat( ">>", attributeNodeStack.depth() + 2 ),
					subTypeName
			);
		}

		final AttributeNodeImplementor<?> attributeNode = attributeNodeStack.getCurrent();
		final SubGraphGenerator subGraphCreator = graphSourceStack.getCurrent();

		final SubGraphImplementor<?> subGraph = subGraphCreator.createSubGraph(
				attributeNode,
				subTypeName,
				sessionFactory
		);

		graphStack.push( subGraph );

		try {
			ctx.attributeList().accept( this );
		}
		finally {
			graphStack.pop();
		}

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Finished graph : %s",
					StringHelper.repeat( "<<", attributeNodeStack.depth() + 2 ),
					subGraph.getGraphedType().getDomainTypeName()
			);
		}

		return subGraph;
	}
}
