/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.select;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.SqmVisitableNode;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * Defines a SQM AST node that can be used as a selection in the query,
 * or as an argument to a dynamic-instantiation.
 *
 * @author Steve Ebersole
 */
public interface SqmSelectableNode extends SqmTypedNode, SqmVisitableNode {
	/**
	 * The expectation is that the walking method for SqmSelectableNode
	 * will return some reference to a
	 * {@link DomainResultProducer} which can be
	 * used to generate a {@link DomainResult}
	 * for this selection in the SQM query
	 */
	@Override
	<T> T accept(SemanticQueryWalker<T> walker);
}
