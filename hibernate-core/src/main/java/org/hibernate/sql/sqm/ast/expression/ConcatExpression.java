/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.expression;

import org.hibernate.sql.sqm.convert.spi.SqlTreeWalker;
import org.hibernate.type.BasicType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class ConcatExpression extends SelfReadingExpressionSupport {
	private final Expression lhsOperand;
	private final Expression rhsOperand;
	private final BasicType type;

	public ConcatExpression(Expression lhsOperand, Expression rhsOperand) {
		this( lhsOperand, rhsOperand, (BasicType) lhsOperand.getType() );
	}

	public ConcatExpression(Expression lhsOperand, Expression rhsOperand, BasicType type) {
		this.lhsOperand = lhsOperand;
		this.rhsOperand = rhsOperand;
		this.type = type;
	}

	public Expression getLeftHandOperand() {
		return lhsOperand;
	}

	public Expression getRightHandOperand() {
		return rhsOperand;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public void accept(SqlTreeWalker sqlTreeWalker) {
		sqlTreeWalker.visitConcatExpression( this );
	}
}
