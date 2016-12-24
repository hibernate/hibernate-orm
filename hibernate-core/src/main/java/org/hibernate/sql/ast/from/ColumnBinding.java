/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.from;

import java.util.Locale;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.sql.ast.select.SqlSelectable;
import org.hibernate.sql.exec.results.process.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.exec.results.process.spi.SqlSelectionReader;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.BasicType;

/**
 * Represents a binding of a column (derived or physical) into a SQL statement
 *
 * @author Steve Ebersole
 */
public class ColumnBinding implements SqlSelectable {
	private final String identificationVariable;
	private final Column column;
	private final SqlSelectionReader sqlSelectionReader;

	public ColumnBinding(Column column, BasicType type, TableBinding tableBinding) {
		this.identificationVariable = tableBinding.getIdentificationVariable();
		this.column = column;
		this.sqlSelectionReader = new SqlSelectionReaderImpl( type );
	}

	public ColumnBinding(Column column, int jdbcTypeCode, TableBinding tableBinding) {
		this.identificationVariable = tableBinding.getIdentificationVariable();
		this.column = column;
		this.sqlSelectionReader = new SqlSelectionReaderImpl( jdbcTypeCode );
	}

	public ColumnBinding(Column column, TableBinding tableBinding) {
		this( column, column.getJdbcType(), tableBinding );
	}

	public Column getColumn() {
		return column;
	}

	public String getIdentificationVariable() {
		return identificationVariable;
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return sqlSelectionReader;
	}

	@Override
	public void accept(SqlAstSelectInterpreter interpreter) {
		interpreter.visitColumnBinding( this );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final ColumnBinding that = (ColumnBinding) o;
		return getIdentificationVariable().equals( that.getIdentificationVariable() )
				&& getColumn().equals( that.getColumn() );
	}

	@Override
	public int hashCode() {
		int result = getIdentificationVariable().hashCode();
		result = 31 * result + getColumn().hashCode();
		return result;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT, 
				"ColumnBinding(%s.%s)",
				getIdentificationVariable(),
				column.getExpression()
		);
	}
}
