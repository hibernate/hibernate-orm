/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.Locale;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class ColumnReference implements Expression {
	private final ColumnReferenceQualifier qualifier;
	private final Column column;

	public ColumnReference(ColumnReferenceQualifier qualifier, Column column) {
		this.qualifier = qualifier;
		this.column = column;
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return new SqlSelectionImpl(
				jdbcPosition,
				this,
				new SqlSelectionReaderImpl( column.getSqlTypeDescriptor().getJdbcTypeCode() )
		);
	}

	public ColumnReferenceQualifier getQualifier() {
		return qualifier;
	}

	public Column getColumn() {
		return column;
	}

	@Override
	public void accept(SqlAstWalker  interpreter) {
		interpreter.visitColumnReference( this );
	}

	@Override
	public ExpressableType getType() {
		// n/a
		return null;
	}

	@Override
	public SqlExpressable getExpressable() {
		return getColumn();
	}

	public String renderSqlFragment() {
		final TableReference tableReference = qualifier.locateTableReference( column.getSourceTable() );
		return column.render( tableReference.getIdentificationVariable() );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ColumnReference that = (ColumnReference) o;
		return qualifier.equals( that.qualifier )
				&& getColumn().equals( that.getColumn() );
	}

	@Override
	public int hashCode() {
		int result = qualifier.hashCode();
		result = 31 * result + getColumn().hashCode();
		return result;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"%s(%s.%s)",
				getClass().getSimpleName(),
				qualifier,
				column.getExpression()
		);
	}
}
