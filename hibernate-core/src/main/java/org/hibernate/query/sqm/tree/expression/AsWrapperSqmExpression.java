/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;

public class AsWrapperSqmExpression<T> extends AbstractSqmExpression<T> {
	private final SqmExpression<T> expression;

	public AsWrapperSqmExpression(SqmExpression<T> expression, Class<T> type) {
		super(
				expression.nodeBuilder().getTypeConfiguration().getBasicTypeForJavaType( type ),
				expression.nodeBuilder()
		);
		this.expression = expression;
	}

	AsWrapperSqmExpression(SqmExpressible<T> type, SqmExpression<T> expression) {
		super( type, expression.nodeBuilder() );
		this.expression = expression;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitAsWrapperExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "cast(" );
		expression.appendHqlString( sb );
		sb.append( " as " );
		sb.append( getNodeType().getTypeName() );
		sb.append( ')' );
	}

	@Override
	public SqmExpression<T> copy(SqmCopyContext context) {
		return new AsWrapperSqmExpression( getExpressible(), expression.copy( context ) );
	}

	public SqmExpression<?> getExpression() {
		return expression;
	}
}
