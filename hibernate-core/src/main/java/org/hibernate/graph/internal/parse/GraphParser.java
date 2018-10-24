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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.internal.util.io.CharSequenceReader;

import org.jboss.logging.Logger;

import antlr.RecognitionException;
import antlr.Token;
import antlr.TokenStreamException;

/**
 * @author Steve Ebersole
 */
public class GraphParser extends GeneratedGraphParser {
	public static final Logger PARSING_LOGGER = Logger.getLogger( "org.hibernate.orm.graph.parsing" );

	/**
	 * Parse the passed graph textual representation into the passed Graph.
	 */
	public static void parseInto(
			GraphImplementor<?> targetGraph,
			CharSequence graphString,
			SessionFactoryImplementor sessionFactory) {
		final GraphParser instance = new GraphParser( graphString, sessionFactory );
		instance.graphStack.push( targetGraph );
		try {
			instance.graph();
		}
		catch (RecognitionException | TokenStreamException e) {
			throw new InvalidGraphException( "Error parsing graph string" );
		}
	}

	private final SessionFactoryImplementor sessionFactory;

	private final Stack<GraphImplementor<?>> graphStack = new StandardStack<>();
	private final Stack<AttributeNodeImplementor<?>> attributeNodeStack = new StandardStack<>();
	private final Stack<SubGraphGenerator> graphSourceStack = new StandardStack<>();

	private GraphParser(CharSequence charSequence, SessionFactoryImplementor sessionFactory) {
		super( new GraphLexer( new CharSequenceReader( charSequence ) ) );
		this.sessionFactory = sessionFactory;
	}


	@Override
	protected void startAttribute(Token attributeNameToken) {
		final String attributeName = attributeNameToken.getText();

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Start attribute : %s",
					StringHelper.repeat( ">>", attributeNodeStack.depth() + 1 ),
					attributeName
			);
		}

		final AttributeNodeImplementor attributeNode = resolveAttributeNode( attributeName );
		attributeNodeStack.push( attributeNode );

		graphSourceStack.push( PathQualifierType.VALUE.getSubGraphCreator() );
	}

	private AttributeNodeImplementor resolveAttributeNode(String attributeName) {
		final GraphImplementor<?> currentGraph = graphStack.getCurrent();
		assert currentGraph != null;

		final AttributeNodeImplementor attributeNode = currentGraph.addAttributeNode( attributeName );
		assert attributeNode != null;

		return attributeNode;
	}

	@Override
	protected void startQualifiedAttribute(Token attributeNameToken, Token qualifierToken) {
		final String attributeName = attributeNameToken.getText();
		final String qualifierName = qualifierToken.getText();

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Start qualified attribute : %s.%s",
					StringHelper.repeat( ">>", attributeNodeStack.depth() + 1 ),
					attributeName,
					qualifierName
			);
		}

		final AttributeNodeImplementor<?> attributeNode = resolveAttributeNode( attributeName );
		attributeNodeStack.push( attributeNode );

		final PathQualifierType pathQualifierType = resolvePathQualifier( qualifierName );

		graphSourceStack.push( pathQualifierType.getSubGraphCreator() );
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
	protected void finishAttribute() {
		graphSourceStack.pop();

		final AttributeNodeImplementor<?> popped = attributeNodeStack.pop();

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Finished attribute : %s",
					StringHelper.repeat( "<<", attributeNodeStack.depth() + 1 ),
					popped.getAttributeDescriptor().getName()
			);
		}
	}

	@Override
	protected void startSubGraph(Token subTypeToken) {
		final String subTypeName = subTypeToken == null ? null : subTypeToken.getText();

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Starting graph : %s",
					StringHelper.repeat( ">>", attributeNodeStack.depth() + 2 ),
					subTypeName
			);
		}

		final AttributeNodeImplementor<?> attributeNode = attributeNodeStack.getCurrent();
		graphStack.push(
				graphSourceStack.getCurrent()
						.createSubGraph( attributeNode, subTypeName, sessionFactory )
		);
	}

	@Override
	protected void finishSubGraph() {
		final GraphImplementor<?> popped = graphStack.pop();

		if ( PARSING_LOGGER.isDebugEnabled() ) {
			PARSING_LOGGER.debugf(
					"%s Finished graph : %s",
					StringHelper.repeat( "<<", attributeNodeStack.depth() + 2 ),
					popped.getGraphedType().getName()
			);
		}
	}
}
