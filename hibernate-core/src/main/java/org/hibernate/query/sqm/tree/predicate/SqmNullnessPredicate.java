/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmNullnessPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression<?> expression;

	public SqmNullnessPredicate(SqmExpression<?> expression, NodeBuilder nodeBuilder) {
		this( expression, false, nodeBuilder );
	}

	public SqmNullnessPredicate(SqmExpression expression, boolean negated, NodeBuilder nodeBuilder) {
		super( negated, nodeBuilder );
		this.expression = expression;
	}

	public SqmExpression getExpression() {
		return expression;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitIsNullPredicate( this );
	}
}
