/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Christian Beikov
 */
public class SqmOverflow<T> extends AbstractSqmExpression<T> {

	private final SqmExpression<T> separatorExpression;
	private final SqmExpression<T> fillerExpression;
	private final boolean withCount;

	public SqmOverflow(SqmExpression<T> separatorExpression, SqmExpression<T> fillerExpression, boolean withCount) {
		super( separatorExpression.getNodeType(), separatorExpression.nodeBuilder() );
		this.separatorExpression = separatorExpression;
		this.fillerExpression = fillerExpression;
		this.withCount = withCount;
	}

	@Override
	public SqmOverflow<T> copy(SqmCopyContext context) {
		final SqmOverflow<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		final SqmOverflow<T> expression = context.registerCopy(
				this,
				new SqmOverflow<>(
						separatorExpression.copy( context ),
						fillerExpression == null ? null : fillerExpression.copy( context ),
						withCount
				)
		);
		copyTo( expression, context );
		return expression;
	}

	public SqmExpression<T> getSeparatorExpression() {
		return separatorExpression;
	}

	public SqmExpression<T> getFillerExpression() {
		return fillerExpression;
	}

	public boolean isWithCount() {
		return withCount;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitOverflow( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		separatorExpression.appendHqlString( sb );
		sb.append( " on overflow " );
		if ( fillerExpression == null ) {
			sb.append( "error" );
		}
		else {
			sb.append( "truncate " );
			fillerExpression.appendHqlString( sb );
			if ( withCount ) {
				sb.append( " with count" );
			}
			else {
				sb.append( " without count" );
			}
		}
	}

}
