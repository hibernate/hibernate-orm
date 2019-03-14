/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.annotations.Remove;
import org.hibernate.internal.util.Loggable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Models the QueryResultProducer generated as part of walking an SQM AST
 * to generate an SQL AST.
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
		return getNavigablePath().toLoggableFragment() + "(" + getColumnReferenceQualifier() + ")";

	}

	/**
	 * Corollary to {@link Navigable#getContainer()} on the reference/expression side
	 */
	@Deprecated
	@Remove
	default NavigableContainerReference getNavigableContainerReference() {
		throw new UnsupportedOperationException(  );
	}

	/**
	 * @deprecated Prefer {@link #getAssociatedTableGroup()} instead
	 * @return
	 */
	@Deprecated
	@Remove
	ColumnReferenceQualifier getColumnReferenceQualifier();

	default TableGroup getAssociatedTableGroup() {
		return (TableGroup) getColumnReferenceQualifier();
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
