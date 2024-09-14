/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

/**
 * @author Gavin King
 */
public class SqmDistinct<T> extends AbstractSqmNode implements SqmTypedNode<T> {

	private final SqmExpression<T> expression;

	public SqmDistinct(SqmExpression<T> expression, NodeBuilder builder) {
		super( builder );
		this.expression = expression;
	}

	@Override
	public SqmDistinct<T> copy(SqmCopyContext context) {
		final SqmDistinct<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy(
				this,
				new SqmDistinct<>(
						expression.copy( context ),
						nodeBuilder()
				)
		);
	}

	public SqmExpression<T> getExpression() {
		return expression;
	}

	@Override
	public SqmExpressible<T> getNodeType() {
		return expression.getNodeType();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitDistinct(this);
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "distinct " );
		expression.appendHqlString( sb );
	}
}
