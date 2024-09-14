/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

/**
 * Entity type expression based on a parameter - `TYPE( :someParam )`
 *
 * @author Steve Ebersole
 */
public class SqmParameterizedEntityType<T> extends AbstractSqmExpression<T> implements SqmSelectableNode<T> {
	private final SqmParameter<T> discriminatorSource;

	public SqmExpression getDiscriminatorSource() {
		return discriminatorSource;
	}

	public SqmParameterizedEntityType(SqmParameter<T> parameterExpression, NodeBuilder nodeBuilder) {
		super( SqmExpressionHelper.toSqmType( parameterExpression.getAnticipatedType(), nodeBuilder ), nodeBuilder );
		this.discriminatorSource = parameterExpression;
	}

	@Override
	public SqmParameterizedEntityType<T> copy(SqmCopyContext context) {
		final SqmParameterizedEntityType<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmParameterizedEntityType<T> expression = context.registerCopy(
				this,
				new SqmParameterizedEntityType<>(
						discriminatorSource.copy( context ),
						nodeBuilder()
				)
		);
		copyTo( expression, context );
		return expression;
	}

	@Override
	public void internalApplyInferableType(SqmExpressible<?> type) {
		setExpressibleType( type );

		//noinspection unchecked
		discriminatorSource.applyInferableType( type );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitParameterizedEntityTypeExpression( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "type(" );
		discriminatorSource.appendHqlString( sb );
		sb.append( ')' );
	}

}
