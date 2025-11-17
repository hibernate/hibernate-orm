/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import java.util.Iterator;
import java.util.List;

import org.hibernate.Internal;
import org.hibernate.internal.log.SubSystemLogging;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.sql.results.ResultsLogger;

import org.jboss.logging.Logger;

import static org.hibernate.sql.results.graph.DomainResultGraphPrinter.Logging.AST_LOGGER;

/**
 * Printer for DomainResult graphs
 *
 * @author Steve Ebersole
 */
public class DomainResultGraphPrinter {
	@SubSystemLogging(
			name = Logging.LOGGER_NAME,
			description = "Logging of DomainResult graphs"
	)
	@Internal
	interface Logging {
		String LOGGER_NAME = ResultsLogger.LOGGER_NAME + ".graph.AST";
		Logger AST_LOGGER = Logger.getLogger( LOGGER_NAME );
	}

	public static void logDomainResultGraph(List<DomainResult<?>> domainResults) {
		logDomainResultGraph( "DomainResult graph", domainResults );
	}

	public static void logDomainResultGraph(String header, List<DomainResult<?>> domainResults) {
		if ( AST_LOGGER.isTraceEnabled() ) {
			new DomainResultGraphPrinter( header ).visitDomainResults( domainResults );
		}
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

		AST_LOGGER.trace( buffer.toString() );
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
					.append( "]" );
		}
		buffer.append( '\n' );

		if ( node instanceof FetchParent fetchParent ) {
			visitFetches( fetchParent );
		}
	}

	private void visitFetches(FetchParent fetchParent) {
		fetchParentStack.push( fetchParent );

		try {
//			final Fetch identifierFetch = fetchParent.getKeyFetch();
//			if ( identifierFetch != null ) {
//				final boolean lastInBranch = identifierFetch.getFetchedMapping() instanceof FetchParent;
//				visitKeyGraphNode( identifierFetch, lastInBranch );
//			}

			for ( Iterator<Fetch> iterator = fetchParent.getFetches().iterator(); iterator.hasNext(); ) {
				final Fetch fetch = iterator.next();
				final boolean lastInBranch = !iterator.hasNext();
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
