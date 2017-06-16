/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.exec.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.internal.BasicValuedNonNavigableSelection;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class CoalesceFunction implements StandardFunction {
	private List<Expression> values = new ArrayList<>();

	public List<Expression> getValues() {
		return values;
	}

	public void value(Expression expression) {
		values.add( expression );
	}

	@Override
	public BasicType getType() {
		return (BasicType) values.get( 0 ).getType();
	}

	@Override
	public void accept(SqlAstWalker  walker) {
		walker.visitCoalesceFunction( this );
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public Selection createSelection(Expression selectedExpression, String resultVariable) {
		assert selectedExpression == this;

		return new BasicValuedNonNavigableSelection( selectedExpression, resultVariable, this );
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return new SqlSelectionReaderImpl( getType() );
	}
}
