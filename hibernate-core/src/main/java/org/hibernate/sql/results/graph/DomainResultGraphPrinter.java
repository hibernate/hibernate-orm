/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.List;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;

import org.jboss.logging.Logger;

/**
 * Printer for DomainResult graphs
 *
 * todo (6.0) : implement / use this
 *
 * @author Steve Ebersole
 */
public class DomainResultGraphPrinter {
	private static final Logger log = Logger.getLogger( DomainResultGraphPrinter.class );

	public static void print(List<DomainResult> domainResults) {
		if ( ! log.isDebugEnabled() ) {
			return;
		}

		final DomainResultGraphPrinter graphPrinter = new DomainResultGraphPrinter();
		graphPrinter.visitDomainResults( domainResults );
	}

	private final StringBuilder buffer = new StringBuilder( "DomainResult Graph:\n");
	private final Stack<FetchParent> fetchParentStack = new StandardStack<>();

	private DomainResultGraphPrinter() {
	}

	private void visitDomainResults(List<DomainResult> domainResults) {
		for ( int i = 0; i < domainResults.size(); i++ ) {
			final DomainResult<?> domainResult = domainResults.get( i );
			// DomainResults should always be the base for a branch
			assert fetchParentStack.isEmpty();

			final boolean lastInBranch = i + 1 == domainResults.size();

			visitGraphNode( domainResult, lastInBranch );
		}

		log.debug( buffer.toString() );
	}

	private void visitGraphNode(DomainResultGraphNode node, boolean lastInBranch) {
		indentLine();

		if ( lastInBranch ) {
			buffer.append( " \\-" );
		}
		else {
			buffer.append( " +-" );
		}

		buffer.append( node.getClass().getSimpleName() );
		if ( node.getNavigablePath() != null ) {
			buffer.append( " [" )
					.append( node.getNavigablePath().getFullPath() )
					.append( "]" );
		}
		buffer.append( '\n' );

		if ( node instanceof FetchParent ) {
			visitFetches( (FetchParent) node );
		}
	}

	private void visitFetches(FetchParent fetchParent) {
		fetchParentStack.push( fetchParent );

		try {
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
