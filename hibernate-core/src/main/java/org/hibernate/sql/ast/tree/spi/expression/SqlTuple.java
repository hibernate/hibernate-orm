/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.util.List;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class SqlTuple implements Expression {
	private final List<Expression> expressions;

	public SqlTuple(List expressions) {
		this.expressions = expressions;
	}

	@Override
	public SqlExpressableType getType() {
		// todo (6.0) : what to return here?
		return null;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition, BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		return null;
	}

	public List<Expression> getExpressions(){
		return expressions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTuple( this );
	}
}
