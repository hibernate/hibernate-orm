/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.path.spi;

import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.spi.ParsingContext;
import org.hibernate.query.sqm.produce.spi.QuerySpecProcessingState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNavigableJoinBuilder implements NavigableJoinBuilder {
	@Override
	public void buildNavigableJoinIfNecessary(
			SqmNavigableReference navigableReference,
			boolean isTerminal) {
		if ( ! isJoinable( navigableReference ) ) {
			return;
		}

		if ( !SqmFromExporter.class.isInstance( navigableReference  )) {
			return;
		}

		if ( navigableReference.getExportedFromElement() != null ) {
			return;
		}

		if ( !isTerminal || forceTerminalJoin() ) {
			// NOTE : the join injects itself into the attribute reference as part
			// of its instantiation
			getQuerySpecProcessingState().getFromElementBuilder().buildNavigableJoin(
					navigableReference,
					isTerminal ? getTerminalJoinAlias() : null,
					null,
					getJoinType(),
					isFetched(),
					canReuseJoins()
			);
		}

	}

	protected abstract ParsingContext getParsingContext();
	protected abstract QuerySpecProcessingState getQuerySpecProcessingState();

	protected  boolean forceTerminalJoin() {
		return false;
	}

	protected String getTerminalJoinAlias() {
		return null;
	}

	protected SqmJoinType getJoinType() {
		return SqmJoinType.INNER;
	}

	protected boolean isFetched() {
		return false;
	}

	protected boolean canReuseJoins() {
		return true;
	}
}
