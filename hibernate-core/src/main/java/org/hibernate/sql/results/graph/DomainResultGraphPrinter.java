/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.List;

import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.sql.results.ResultsLogger;

import org.jboss.logging.Logger;

import static org.hibernate.sql.results.graph.DomainResultGraphPrinter.Logging.AST_LOGGER;
import static org.hibernate.sql.results.graph.DomainResultGraphPrinter.Logging.TRACE_ENABLED;

/**
 * Printer for DomainResult graphs
 *
 * @author Steve Ebersole
 */
public class DomainResultGraphPrinter {
	@SubSystemLogging(
			name = Logging.LOGGER_NAME,
			description = "Logging of `DomainResult` graphs"
	)
	interface Logging {
		String LOGGER_NAME = ResultsLogger.LOGGER_NAME + ".graph.AST";
		Logger AST_LOGGER = Logger.getLogger( LOGGER_NAME );
		boolean DEBUG_ENABLED = AST_LOGGER.isDebugEnabled();
		boolean TRACE_ENABLED = AST_LOGGER.isTraceEnabled();
	}

	public static void logDomainResultGraph(List<DomainResult<?>> domainResults) {
		logDomainResultGraph( "DomainResult Graph", domainResults );
	}

	public static void logDomainResultGraph(String header, List<DomainResult<?>> domainResults) {
		if ( ! Logging.DEBUG_ENABLED ) {
			return;
		}

		final DomainResultGraphPrinter graphPrinter = new DomainResultGraphPrinter( header );
		graphPrinter.visitDomainResults( domainResults );
	}

	private final StringBuilder buffer;
	private final Stack<FetchParent> fetchParentStack = new StandardStack<>();

	private DomainResultGraphPrinter(String header) {
		buffer = new StringBuilder( header + ":" + System.lineSeparator() );
	}

	private void visitDomainResults(List<DomainResult<?>> domainResults) {
		for ( int i = 0; i < domainResults.size(); i++ ) {
			final DomainResult<?> domainResult = domainResults.get( i );
			// DomainResults should always be the base for a branch
			assert fetchParentStack.isEmpty();

			final boolean lastInBranch = i + 1 == domainResults.size();

			visitGraphNode( domainResult, lastInBranch );
		}

		AST_LOGGER.debug( buffer.toString() );

		if ( TRACE_ENABLED ) {
			AST_LOGGER.tracef( new Exception(), "Stack trace calling DomainResultGraphPrinter" );
		}
	}

	private void visitGraphNode(DomainResultGraphNode node, boolean lastInBranch) {
		visitGraphNode( node, lastInBranch, node.getClass().getSimpleName() );
	}

	private void visitGraphNode(DomainResultGraphNode node, boolean lastInBranch, String nodeText) {
		indentLine();

		if ( lastInBranch ) {
			buffer.append( " \\-" );
		}
		else {
			buffer.append( " +-" );
		}

		buffer.append( nodeText );
		if ( node.getNavigablePath() != null ) {
			buffer.append( " [" )
					.append( node.getNavigablePath().getFullPath() )
					.append( ']' );
		}
		buffer.append( '\n' );

		if ( node instanceof FetchParent ) {
			visitFetches( (FetchParent) node );
		}
	}

	private void visitKeyGraphNode(DomainResultGraphNode node, boolean lastInBranch) {
		visitGraphNode( node, lastInBranch, "(key) " + node.getClass().getSimpleName() );
	}

	private void visitFetches(FetchParent fetchParent) {
		fetchParentStack.push( fetchParent );

		try {
//			final Fetch identifierFetch = fetchParent.getKeyFetch();
//			if ( identifierFetch != null ) {
//				final boolean lastInBranch = identifierFetch.getFetchedMapping() instanceof FetchParent;
//				visitKeyGraphNode( identifierFetch, lastInBranch );
//			}

			final int numberOfFetches = fetchParent.getFetches().size();

			for ( int i = 0; i < numberOfFetches; i++ ) {
				final Fetch fetch = fetchParent.getFetches().get( i );

				final boolean lastInBranch = i + 1 == numberOfFetches;
				visitGraphNode( fetch, lastInBranch );
			}
		}
		finally {
			fetchParentStack.pop();
		}
	}

	private void indentLine() {
		fetchParentStack.visitRootFirst(
				fetchParent -> {
					final boolean hasSubFetches = ! fetchParent.getFetches().isEmpty();
					if ( hasSubFetches ) {
						buffer.append( " | " );
					}
					else {
						buffer.append( "   " );
					}
				}
		);
	}
}
