/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.expression;

import java.util.Locale;
import java.util.Objects;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
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
	private final String columnExpression;
	private final String qualifier;
	private final JdbcMapping jdbcMapping;

	public ColumnReference(
			String columnExpression,
			String qualifier,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this.columnExpression = columnExpression;
		this.qualifier = qualifier;
		this.jdbcMapping = jdbcMapping;
	}

	public String getReferencedColumnExpression() {
		return columnExpression;
	}

	public String getQualifier() {
		return qualifier;
	}

	@Override
	public MappingModelExpressable getExpressionType() {
		return (MappingModelExpressable) jdbcMapping;
	}

	@Override
	public void accept(SqlAstWalker interpreter) {
		interpreter.visitColumnReference( this );
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {

		// todo (6.0) : potential use for runtime database model - interpretation of table and column references
		//		into metadata info such as java/sql type, binder, extractor

		return new SqlSelectionImpl( jdbcPosition, valuesArrayPosition, this, jdbcMapping );
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"%s(%s)",
				getClass().getSimpleName(),
				columnExpression
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

		final ColumnReference that = (ColumnReference) o;
		return Objects.equals( qualifier, that.qualifier )
				&& Objects.equals( columnExpression, that.columnExpression );
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash( columnExpression );
		return qualifier == null ? hash : hash + Objects.hash( qualifier );
	}

	public String renderSqlFragment(SessionFactoryImplementor sessionFactory) {
		if ( getQualifier() != null ) {
			return getQualifier() + '.' + getReferencedColumnExpression();
		}

		return getReferencedColumnExpression();
	}
}
