/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class ModFunction extends AbstractStandardFunction implements StandardFunction {
	private final Expression dividend;
	private final Expression divisor;
	private final SqlExpressableType type;

	public ModFunction(Expression dividend, Expression divisor) {
		this( dividend, divisor, dividend.getType() );
	}

	public ModFunction(
			Expression dividend,
			Expression divisor,
			SqlExpressableType type) {
		this.dividend = dividend;
		this.divisor = divisor;
		this.type = type;
	}

	public Expression getDividend() {
		return dividend;
	}

	public Expression getDivisor() {
		return divisor;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitModFunction( this );
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlExpressableType getType() {
		return type;
	}
}
