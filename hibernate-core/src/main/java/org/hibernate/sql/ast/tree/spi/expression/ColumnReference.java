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
import org.hibernate.sql.ast.produce.ConversionException;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.Selectable;
import org.hibernate.sql.results.internal.SqlSelectionReaderImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionReader;

/**
 * @author Steve Ebersole
 */
public class ColumnReference implements Expression {
	private final String identificationVariable;
	private final Column column;
	private final SqlSelectionReader sqlSelectionReader;


	public ColumnReference(Column column, BasicValuedExpressableType type, TableReference tableReference) {
		this.identificationVariable = tableReference.getIdentificationVariable();
		this.column = column;
		this.sqlSelectionReader = new SqlSelectionReaderImpl( type );
	}

	public ColumnReference(Column column, int jdbcTypeCode, TableReference tableReference) {
		this.identificationVariable = tableReference.getIdentificationVariable();
		this.column = column;
		this.sqlSelectionReader = new SqlSelectionReaderImpl( jdbcTypeCode );
	}

	public ColumnReference(Column column, TableReference tableReference) {
		this( column, column.getJdbcType(), tableReference );
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
	public void accept(SqlAstWalker  interpreter) {
		interpreter.visitColumnReference( this );
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


	@Override
	public ExpressableType getType() {
		// n/a
		return null;
	}

	@Override
	public QueryResult createQueryResult(
			SqlSelection sqlSelection,
			String resultVariable,
			SqlExpressionResolver sqlSelectionResolver,
			QueryResultCreationContext creationContext) {
		return null;
	}

	@Override
	public Selectable getSelectable() {
		throw new ConversionException( "ColumnReferenceExpression is not Selectable" );
	}
}
