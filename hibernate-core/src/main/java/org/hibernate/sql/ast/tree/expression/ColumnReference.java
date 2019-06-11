/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.expression;

import java.util.Locale;
import java.util.Objects;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.persister.SqlExpressableType;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.exec.spi.JdbcValueExtractor;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models a reference to a Column in a SQL AST
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements Expression {
	private final Identifier columnName;
	private final SqlExpressableType sqlExpressableType;

	private String sqlFragment;

	public ColumnReference(ColumnReferenceQualifier qualifier, Identifier columnName, SqlExpressableType sqlExpressableType) {
		// the assumption with this assertion is that callers are expecting there
		// to be a qualifier; otherwise, they would call the overload ctor form
		// not accepting a qualifier
		assert qualifier != null : "ColumnReferenceQualifier is null";

		this.columnName = columnName;
		this.sqlExpressableType = sqlExpressableType;
		this.sqlFragment = renderSqlFragment( qualifier, columnName );
	}

	private static String renderSqlFragment(ColumnReferenceQualifier qualifier, Identifier columnName) {
		if ( qualifier == null ) {
			return columnName.render();
		}
		else {
			final TableReference tableReference = qualifier.locateTableReference( column.getSourceTable() );
			return columnName.render( tableReference.getIdentificationVariable() );
		}
	}

	public ColumnReference(Identifier columnName, SqlExpressableType sqlExpressableType) {
		this.columnName = columnName;
		this.sqlExpressableType = sqlExpressableType;
		this.sqlFragment = renderSqlFragment( null, columnName );
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaTypeDescriptor<?> javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		final JdbcValueExtractor jdbcValueExtractor = sqlExpressableType.getJdbcValueExtractor();
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				jdbcValueExtractor
		);
	}

	public Identifier getColumnName() {
		return columnName;
	}

	@Override
	public void accept(SqlAstWalker interpreter) {
		interpreter.visitColumnReference( this );
	}

	@Override
	public SqlExpressableType getType() {
		return sqlExpressableType;
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
				columnName.getCanonicalName()
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
