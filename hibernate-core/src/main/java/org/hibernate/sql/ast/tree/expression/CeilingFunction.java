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
public class CeilingFunction extends AbstractFunction {
	private final Expression argument;
	private SqlExpressableType type;

	public CeilingFunction(Expression argument, SqlExpressableType type) {
		this.argument = argument;
		this.type = type;
	}

	@Override
	public SqlExpressableType getExpressableType() {
		return type;
	}

	public Expression getArgument() {
		return argument;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitCeilingFunction( this );
	}

	@Override
	public SqlExpressableType getType() {
		return type;
	}

}
