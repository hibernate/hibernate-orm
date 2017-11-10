/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.util.List;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class SqlTuple implements Expression {
	private final List<Expression> expressions;

	public SqlTuple(List<Expression> expressions) {
		this.expressions = expressions;
	}

	@Override
	public ExpressableType getType() {
		// todo (6.0) : what to return here?
		return null;
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return null;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {

	}
}
