/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class ConcatFunction implements StandardFunction {
	private final List<Expression> expressions;
	private final AllowableFunctionReturnType type;

	public ConcatFunction(
			List<Expression> expressions,
			AllowableFunctionReturnType type) {
		this.expressions = expressions;
		this.type = type;
	}

	public List<Expression> getExpressions() {
		return expressions;
	}

	@Override
	public AllowableFunctionReturnType getType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitConcatFunction( this );
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return null;
	}
}
