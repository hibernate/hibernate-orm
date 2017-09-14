/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.QueryResultProducer;

/**
 * Models the QueryResultProducer generated as part of walking an SQM AST
 * to generate an SQL AST.
 *
 * @todo (6.0) : might be better to move this to o.h.query.sqm / o.h.query.sqm.consume
 * 		regardless, this is not a SQL expression.  make sure the SQM-to-SQL
 * 		generation is defined in terms of QueryResultProducer for its
 * 		selection handling
 *
 * @see org.hibernate.query.sqm.tree.select.SqmSelectableNode#accept
 *
 * @author Steve Ebersole
 */
public interface NavigableReference extends QueryResultProducer {

	@Override
	default QueryResult createQueryResult(
			String resultVariable,
			QueryResultCreationContext creationContext) {
		return getNavigable().createQueryResult(
				this,
				resultVariable,
				creationContext
		);
	}

	ColumnReferenceQualifier getSqlExpressionQualifier();

	/**
	 * Get the Navigable referenced by this expression
	 *
	 * @return The Navigable
	 */
	Navigable getNavigable();

	NavigablePath getNavigablePath();

	/**
	 * Corollary to {@link Navigable#getContainer()} on the reference/expression side
	 */
	NavigableContainerReference getNavigableContainerReference();
}
