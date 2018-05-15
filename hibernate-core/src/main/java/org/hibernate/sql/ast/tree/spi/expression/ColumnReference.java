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
 * @author Steve Ebersole
 */
public class ColumnReference implements Expression {
	private String sqlFragment;
	private final Column column;

	public ColumnReference(ColumnReferenceQualifier qualifier, Column column) {
		assert qualifier != null;
		this.column = column;
		renderSqlFragment(qualifier);
	}

	public ColumnReference(Column column) {
		this.column = column;
		renderSqlFragment(null);
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		final JdbcValueExtractor jdbcValueExtractor = getColumn().getSqlTypeDescriptor().getSqlExpressableType(
				column.getSqlTypeDescriptor().getJdbcRecommendedJavaTypeMapping( typeConfiguration ),
				typeConfiguration
		).getJdbcValueExtractor();
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
		// n/a
		return null;
	}

	@Override
	public SqlExpressable getExpressable() {
		return getColumn();
	}

	private void renderSqlFragment(ColumnReferenceQualifier qualifier) {
		if ( qualifier == null ) {
			sqlFragment = column.render();
		}

else {
			final TableReference tableReference = qualifier.locateTableReference( column.getSourceTable() );
			sqlFragment = column.render( tableReference.getIdentificationVariable() );
		}
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
