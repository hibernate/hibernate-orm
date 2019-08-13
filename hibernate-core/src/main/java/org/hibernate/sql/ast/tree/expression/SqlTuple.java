/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.ValueMappingExpressable;
import org.hibernate.sql.ast.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class SqlTuple implements Expression {
	private final List<? extends Expression> expressions;
	private final ValueMappingExpressable expressable;

	public SqlTuple(List<? extends Expression> expressions, ValueMappingExpressable expressable) {
		this.expressions = expressions;
		this.expressable = expressable;
	}

	public SqlTuple(Expression expression, ValueMappingExpressable expressable) {
		this( Collections.singletonList( expression ), expressable );
	}

	@Override
	public ValueMappingExpressable getExpressionType() {
		return expressable;
	}

	public List<? extends Expression> getExpressions(){
		return expressions;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTuple( this );
	}
}
