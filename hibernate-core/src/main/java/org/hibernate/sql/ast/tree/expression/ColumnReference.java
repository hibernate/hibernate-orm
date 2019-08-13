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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.mapping.spi.SqlExpressableType;
import org.hibernate.sql.ast.ValueMappingExpressable;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.descriptor.ValueExtractor;
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

	private final TableReference tableReference;

	private final String sqlFragment;

	public ColumnReference(
			Identifier columnName,
			TableReference tableReference,
			SqlExpressableType sqlExpressableType,
			SessionFactoryImplementor sessionFactory) {
		this.columnName = columnName;
		this.tableReference = tableReference;
		this.sqlExpressableType = sqlExpressableType;

		final String renderedColumnRef = columnName.render(
				sessionFactory.getJdbcServices().getJdbcEnvironment().getDialect()
		);

		this.sqlFragment = tableReference.getIdentificationVariable() == null
				? renderedColumnRef
				: tableReference.getIdentificationVariable() + '.' + renderedColumnRef;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {

		// todo (6.0) : potential use for runtime database model - interpretation of table and column references
		//		into metadata info such as java/sql type, binder, extractor

		final ValueExtractor jdbcValueExtractor = sqlExpressableType.getJdbcValueExtractor();

		return new SqlSelectionImpl( jdbcPosition, valuesArrayPosition, this, jdbcValueExtractor );
	}

	public Identifier getColumnName() {
		return columnName;
	}

	@Override
	public void accept(SqlAstWalker interpreter) {
		interpreter.visitColumnReference( this );
	}

	@Override
	public ValueMappingExpressable getExpressionType() {
		return (ValueMappingExpressable) sqlExpressableType;
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
