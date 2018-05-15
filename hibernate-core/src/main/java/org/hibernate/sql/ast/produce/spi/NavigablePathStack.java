/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.internal.util.collections.Stack;
import org.hibernate.internal.util.collections.StandardStack;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 * Specialized stack implementation which simultaneously manages the stack of
 * PropertyPath references as well as the logging MDC value based on the current
 * PropertyPath node.
 * <p/>
 * Due to the recursive calls needed for processing a Navigable graph it is generally
 * beneficial to see exactly where we are in the graph walking as part of log messages.
 * This MDC hook provides this capability.
 */
public class NavigablePathStack implements Stack<NavigablePath> {
	private static final Logger log = Logger.getLogger( NavigablePathStack.class );
	private static final boolean TRACE_ENABLED = log.isTraceEnabled();

	private static final String MDC_KEY = "hibernateSqlSelectPlanWalkPath";
	private static final String NO_PATH = "<no-path>";

	private Stack<NavigablePath> internalStack = new StandardStack<>();

	@Override
	public void push(NavigablePath navigablePath) {
		assert navigablePath != null;

		internalStack.push( navigablePath );

		MDC.put( MDC_KEY, navigablePath.getFullPath() );
	}

	public void push(Navigable navigable) {
		assert navigable != null;

		final String navigableName = navigable.getNavigableRole().getNavigableName();
		if ( TRACE_ENABLED ) {
			log.tracef(
					"Pushing Navigable(%s) into NavigablePathStack(%s)",
					navigableName,
					internalStack.getCurrent() != null ? internalStack.getCurrent().getFullPath() : null
			);
		}

		final NavigablePath navigablePath;
		if ( internalStack.isEmpty() ) {
			navigablePath = new NavigablePath( navigableName );
		}
		else {
			navigablePath = internalStack.getCurrent().append( navigableName );
		}

		push( navigablePath );
	}

	public NavigablePath pop() {
		assert !internalStack.isEmpty();

		final NavigablePath previous = internalStack.pop();
		final NavigablePath newHead = internalStack.getCurrent();

		if ( TRACE_ENABLED ) {
			log.tracef(
					"Popped Navigable(%s), new head = NavigablePathStack(%s)",
					previous.getLocalName(),
					newHead == null ? NO_PATH : newHead.getFullPath()
			);
		}

		final String mdcRep = newHead == null ? NO_PATH : newHead.getFullPath();
		MDC.put( MDC_KEY, mdcRep );
		return previous;
	}

	public NavigablePath getCurrent() {
		return internalStack.getCurrent();
	}

	@Override
	public NavigablePath getPrevious() {
		return internalStack.getPrevious();
	}

	@Override
	public void visitCurrentFirst(Consumer<NavigablePath> action) {
		internalStack.visitCurrentFirst( action );
	}

	@Override
	public <X> X findCurrentFirst(Function<NavigablePath, X> function) {
		return internalStack.findCurrentFirst( function );
	}

	@Override
	public int depth() {
		return internalStack.depth();
	}

	@Override
	public boolean isEmpty() {
		return internalStack.isEmpty();
	}

	public void clear() {
		MDC.remove( MDC_KEY );

		if ( !internalStack.isEmpty() ) {
			log.debug( "NavigablePathStack not empty upon completion of visitation; mis-matched push/pop?" );
			internalStack.clear();
		}
	}
}
