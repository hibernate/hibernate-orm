/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi.ast;

import org.hibernate.QueryException;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.sql.results.spi.SqlSelectable;
import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class ScalarExpression implements Expression, Selectable, SqlSelectable {
	private final String columnAlias;
	private final BasicType type;

	private final BasicValuedNonNavigableSelection selection;
	private final SqlSelectionReader reader;

	public ScalarExpression(String columnAlias, BasicType type) {
		this.columnAlias = columnAlias;
		this.type = type;

		this.selection = new BasicValuedNonNavigableSelection( this, columnAlias, this );
		this.reader = type.getSqlSelectionReader();
	}

	@Override
	public ExpressableType getType() {
		return type;
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return reader;
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		return selection;
	}
	@Override

	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new QueryException( "Not expecting SQL AST tree walking" );
	}

}
