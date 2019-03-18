/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression;

import java.util.Locale;
import java.util.Objects;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models a reference to a Column in a SQL AST
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements Expression {
	private final Column column;
	private String sqlFragment;

	public ColumnReference(ColumnReferenceQualifier qualifier, Column column) {
		// the assumption with this assertion is that callers are expecting there
		// to be a qualifier; otherwise, they would call the overload ctor form
		// not accepting a qualifier
		assert qualifier != null : "ColumnReferenceQualifier is null";

		this.column = column;
		this.sqlFragment = renderSqlFragment( qualifier, column );
	}

	private static String renderSqlFragment(ColumnReferenceQualifier qualifier, Column column) {
		if ( qualifier == null ) {
			return column.render();
		}
		else {
			final TableReference tableReference = qualifier.locateTableReference( column.getSourceTable() );
			return column.render( tableReference.getIdentificationVariable() );
		}
	}

	public ColumnReference(Column column) {
		this.column = column;
		this.sqlFragment = renderSqlFragment( null, column );
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		final JdbcValueExtractor jdbcValueExtractor = getColumn().getExpressableType().getJdbcValueExtractor();
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				jdbcValueExtractor
		);
	}

	public Column getColumn() {
		return column;
	}

	@Override
	public void accept(SqlAstWalker interpreter) {
		interpreter.visitColumnReference( this );
	}

	@Override
	public SqlExpressableType getType() {
		return column.getExpressableType();
	}

	public String renderSqlFragment() {
		return this.sqlFragment;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"%s(%s.%s)",
				getClass().getSimpleName(),
				sqlFragment,
				column.getExpression()
		);
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		ColumnReference that = (ColumnReference) o;
		return Objects.equals( sqlFragment, that.sqlFragment );
	}

	@Override
	public int hashCode() {
		return Objects.hash( sqlFragment );
	}
}
