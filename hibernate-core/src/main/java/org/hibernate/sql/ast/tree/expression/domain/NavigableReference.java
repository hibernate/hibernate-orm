/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression.domain;

import org.hibernate.internal.util.Loggable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Models a {@link Navigable} as an "intermediate resolution" of a
 * SQM node as we build the SQL AST
 *
 * @see org.hibernate.query.sqm.tree.select.SqmSelectableNode#accept
 *
 * @author Steve Ebersole
 */
public interface NavigableReference extends DomainResultProducer, Loggable {
	NavigablePath getNavigablePath();

	/**
	 * Get the Navigable referenced by this expression
	 *
	 * @return The Navigable
	 */
	Navigable getNavigable();

	@Override
	default String toLoggableFragment() {
		return getNavigablePath().getFullPath();
	}

	@Override
	default DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return getNavigable().createDomainResult(
				getNavigablePath(),
				resultVariable,
				creationState
		);
	}
}
