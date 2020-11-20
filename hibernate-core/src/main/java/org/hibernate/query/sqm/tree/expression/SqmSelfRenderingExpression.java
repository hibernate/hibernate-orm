/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import java.util.function.Function;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * @author Steve Ebersole
 */
public class SqmSelfRenderingExpression<T> extends AbstractSqmExpression<T> {
	private final Function<SemanticQueryWalker, Expression> renderer;

	public SqmSelfRenderingExpression(
			Function<SemanticQueryWalker, Expression> renderer,
			SqmExpressable<T> type,
			NodeBuilder criteriaBuilder) {
		super( type, criteriaBuilder );
		this.renderer = renderer;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		//noinspection unchecked
		return (X) renderer.apply( walker );
	}
}
