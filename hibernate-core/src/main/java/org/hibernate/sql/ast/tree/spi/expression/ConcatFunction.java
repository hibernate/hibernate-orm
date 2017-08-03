/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import java.util.List;

import org.hibernate.sql.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;

/**
 * @author Steve Ebersole
 */
public class ConcatFunction implements StandardFunction {
	private final List<Expression> expressions;
	private final BasicValuedExpressableType type;

	public ConcatFunction(
			List<Expression> expressions,
			BasicValuedExpressableType type) {
		this.expressions = expressions;
		this.type = type;
	}

	public List<Expression> getExpressions() {
		return expressions;
	}

	@Override
	public BasicValuedExpressableType getType() {
		return type;
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitConcatFunction( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		return new BasicValuedNonNavigableSelection(
				selectedExpression,
				resultVariable,
				this
		);
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}
}
