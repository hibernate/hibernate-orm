/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

/**
 * Models a reference to a Column in a SQL AST
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class ColumnReference implements Expression, Assignable {
	private final String qualifier;
	private final String columnExpression;
	private final boolean isColumnExpressionFormula;
	private final String referenceExpression;
	private final JdbcMapping jdbcMapping;

	public ColumnReference(
			String qualifier,
			String columnExpression,
			boolean isColumnExpressionFormula,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this.qualifier = StringHelper.nullIfEmpty( qualifier );
		if ( isColumnExpressionFormula ) {
			assert qualifier != null;
			this.columnExpression = StringHelper.replace( columnExpression, Template.TEMPLATE, qualifier );
		}
		else {
			this.columnExpression = columnExpression;
		}
		this.isColumnExpressionFormula = isColumnExpressionFormula;
		this.referenceExpression = this.qualifier == null || isColumnExpressionFormula
				? this.columnExpression
				: this.qualifier + "." + this.columnExpression;
		this.jdbcMapping = jdbcMapping;
	}

	public ColumnReference(
			TableReference tableReference,
			String columnExpression,
			boolean isColumnExpressionFormula,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this( tableReference.getIdentificationVariable(), columnExpression, isColumnExpressionFormula, jdbcMapping, sessionFactory );
	}

	public String getQualifier() {
		return qualifier;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	public boolean isColumnExpressionFormula() {
		return isColumnExpressionFormula;
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
		return referenceExpression.equals( that.referenceExpression );
	}

	@Override
	public int hashCode() {
		return referenceExpression.hashCode();
	}

	@Override
	public void visitColumnReferences(Consumer<ColumnReference> columnReferenceConsumer) {
		columnReferenceConsumer.accept( this );
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		return Collections.singletonList( this );
	}
}
