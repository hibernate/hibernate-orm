/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SemanticQueryWalker;

/**
 * @author Christian Beikov
 */
public class SqmCollate<T> extends AbstractSqmExpression<T> {

	private final SqmExpression<T> expression;
	private final String collation;

	public SqmCollate(SqmExpression<T> expression, String collation) {
		super( expression.getNodeType(), expression.nodeBuilder() );
		assert !(expression instanceof SqmTuple);
		this.expression = expression;
		this.collation = collation;
	}

	public SqmExpression<T> getExpression() {
		return expression;
	}

	public String getCollation() {
		return collation;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitCollate( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		expression.appendHqlString( sb );
		sb.append( " collate " );
		sb.append( collation );
	}
}
