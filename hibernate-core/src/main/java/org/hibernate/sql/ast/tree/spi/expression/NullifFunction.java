/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.ast.consume.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class NullifFunction implements StandardFunction {
	private final Expression first;
	private final Expression second;

	public NullifFunction(Expression first, Expression second) {
		this.first = first;
		this.second = second;
	}

	public Expression getFirstArgument() {
		return first;
	}

	public Expression getSecondArgument() {
		return second;
	}

	@Override
	public BasicType getType() {
		return (BasicType) first.getType();
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitNullifFunction( this );
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
