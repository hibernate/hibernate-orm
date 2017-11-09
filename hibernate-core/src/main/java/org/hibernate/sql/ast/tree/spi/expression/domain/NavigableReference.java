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
 * @see org.hibernate.query.sqm.tree.select.SqmSelectableNode#accept
 *
 * @author Steve Ebersole
 */
public interface NavigableReference extends QueryResultProducer {

	// todo (6.0) : I think it might be better to distinguish NavigableReference based on "classification".
	//		E.g.:
	//			* BasicValuedNavigableReference (basic attributes, simple ids, "element collection" elements, etc)
	//			* EntityValuedNavigableReference (root reference, many-to-one, one-to-one, elements of one-to-many, etc)
	//			* CompositeValuedNavigableReference (embedded, composite ids, etc)
	//			* AnyValuedNavigableReference (any, many-to-any, etc)

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

	ColumnReferenceQualifier getSqlExpressionQualifier();

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

}
