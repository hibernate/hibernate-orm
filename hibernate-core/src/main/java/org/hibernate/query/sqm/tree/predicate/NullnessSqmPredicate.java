/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.predicate;

import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class NullnessSqmPredicate extends AbstractNegatableSqmPredicate {
	private final SqmExpression expression;

	public NullnessSqmPredicate(SqmExpression expression) {
		this( expression, false );
	}

	public NullnessSqmPredicate(SqmExpression expression, boolean negated) {
		super( negated );
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
