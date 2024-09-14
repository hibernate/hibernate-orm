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
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Steve Ebersole
 */
public class SqmLiteralNull<T> extends SqmLiteral<T> {

	private static final SqmExpressible<Object> NULL_TYPE = NullSqmExpressible.NULL_SQM_EXPRESSIBLE;

	public SqmLiteralNull(NodeBuilder nodeBuilder) {
		//noinspection unchecked
		this( (SqmExpressible<T>) NULL_TYPE, nodeBuilder );
	}

	public SqmLiteralNull(SqmExpressible<T> expressibleType, NodeBuilder nodeBuilder) {
		super( expressibleType, nodeBuilder );
	}

	@Override
	public SqmLiteralNull<T> copy(SqmCopyContext context) {
		final SqmLiteralNull<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmLiteralNull<T> expression = context.registerCopy(
				this,
				new SqmLiteralNull<>(
						getNodeType(),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitLiteral( this );
	}

	@Override
	public String asLoggableText() {
		return "<literal-null>";
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "null" );
	}
}
