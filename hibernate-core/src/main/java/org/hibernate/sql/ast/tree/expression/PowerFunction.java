/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Gavin King
 */
public class PowerFunction extends AbstractStandardFunction implements StandardFunction {
	private final Expression base;
	private final Expression power;
	private final SqlExpressableType type;

	public PowerFunction(
			Expression base,
			Expression power,
			SqlExpressableType type) {
		this.base = base;
		this.power = power;
		this.type = type;
	}

	public Expression getBase() {
		return base;
	}

	public Expression getPower() {
		return power;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitPowerFunction( this );
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
