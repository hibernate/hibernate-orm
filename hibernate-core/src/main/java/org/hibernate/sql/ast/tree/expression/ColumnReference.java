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
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Models a reference to a Column in a SQL AST
 *
 * @author Steve Ebersole
 */
public class ColumnReference implements Expression {
	private final String referenceExpression;
	private final JdbcMapping jdbcMapping;

	public ColumnReference(
			String qualifier,
			String columnExpression,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this(
				qualifier == null
						? columnExpression
						: qualifier + "." + columnExpression,
				jdbcMapping,
				sessionFactory
		);
	}

	public ColumnReference(
			String referenceExpression,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this.referenceExpression = referenceExpression;
		this.jdbcMapping = jdbcMapping;
	}

	public ColumnReference(
			TableReference tableReference,
			String columnExpression,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this( tableReference.getIdentificationVariable(), columnExpression, jdbcMapping, sessionFactory );
	}

	public String getExpressionText() {
		return referenceExpression;
	}

	public String renderSqlFragment(SessionFactoryImplementor sessionFactory) {
		return getExpressionText();
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
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
				referenceExpression
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
		return Objects.equals( referenceExpression, that.referenceExpression );
	}

	@Override
	public int hashCode() {
		return referenceExpression.hashCode();
	}
}
