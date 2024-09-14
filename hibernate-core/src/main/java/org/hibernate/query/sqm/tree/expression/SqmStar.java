/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Gavin King
 */
public class SqmStar extends AbstractSqmExpression<Object> {

	public SqmStar(NodeBuilder builder) {
		super( null, builder );
	}

	@Override
	public SqmStar copy(SqmCopyContext context) {
		final SqmStar existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}
		return context.registerCopy( this, new SqmStar( nodeBuilder() ) );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitStar( this );
	}
	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( "*" );
	}

}
