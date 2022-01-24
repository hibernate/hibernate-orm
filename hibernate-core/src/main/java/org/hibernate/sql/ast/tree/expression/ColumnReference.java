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
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

import static org.hibernate.metamodel.relational.RuntimeRelationModelHelper.DEFAULT_COLUMN_WRITE_EXPRESSION;

/**
 * Models a reference to a Column in a SQL AST
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class ColumnReference implements Expression, Assignable {
	private final String qualifier;
	private final String columnExpression;
	private final boolean isFormula;
	private final String readExpression;
	private final String writeExpression;
	private final JdbcMapping jdbcMapping;

	public ColumnReference(
			String qualifier,
			SelectableMapping selectableMapping,
			SessionFactoryImplementor sessionFactory) {
		this(
				qualifier,
				selectableMapping.getSelectionExpression(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				selectableMapping.getCustomWriteExpression(),
				selectableMapping.getJdbcMapping(),
				sessionFactory
		);
	}

	public ColumnReference(
			String qualifier,
			SelectableMapping selectableMapping,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this(
				qualifier,
				selectableMapping.getSelectionExpression(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				selectableMapping.getCustomWriteExpression(),
				jdbcMapping,
				sessionFactory
		);
	}

	public ColumnReference(
			String qualifier,
			String columnExpression,
			boolean isFormula,
			String customReadExpression,
			String customWriteExpression,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this.qualifier = StringHelper.nullIfEmpty( qualifier );

		if ( isFormula ) {
			assert qualifier != null;
			this.columnExpression = StringHelper.replace( columnExpression, Template.TEMPLATE, qualifier );
		}
		else {
			this.columnExpression = columnExpression;
		}

		this.isFormula = isFormula;

		if ( isFormula ) {
			this.readExpression = this.columnExpression;
		}
		else if ( customReadExpression != null ) {
			this.readExpression = StringHelper.replace( customReadExpression, Template.TEMPLATE, qualifier );
		}
		else {
			this.readExpression = this.qualifier == null
					? this.columnExpression
					: this.qualifier + "." + this.columnExpression;
		}

		if ( isFormula ) {
			this.writeExpression = null;
		}
		else if ( customWriteExpression != null ) {
			this.writeExpression = StringHelper.replace( customWriteExpression, Template.TEMPLATE, qualifier );
		}
		else {
			this.writeExpression = DEFAULT_COLUMN_WRITE_EXPRESSION;
		}

		this.jdbcMapping = jdbcMapping;
	}

	public ColumnReference(
			TableReference tableReference,
			SelectableMapping selectableMapping,
			SessionFactoryImplementor sessionFactory) {
		this(
				tableReference.getIdentificationVariable(),
				selectableMapping,
				sessionFactory
		);
	}

	public ColumnReference(
			TableReference tableReference,
			String mapping,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this(
				tableReference.getIdentificationVariable(),
				mapping,
				false,
				null,
				null,
				jdbcMapping,
				sessionFactory
		);
	}

	public ColumnReference(
			TableReference tableReference,
			String columnExpression,
			boolean isFormula,
			String customReadExpression,
			String customWriteExpression,
			JdbcMapping jdbcMapping,
			SessionFactoryImplementor sessionFactory) {
		this(
				tableReference.getIdentificationVariable(),
				columnExpression,
				isFormula,
				customReadExpression,
				customWriteExpression,
				jdbcMapping,
				sessionFactory
		);
	}

	public String getQualifier() {
		return qualifier;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	public boolean isColumnExpressionFormula() {
		return isFormula;
	}

	public String getExpressionText() {
		return readExpression;
	}

	public String renderSqlFragment(SessionFactoryImplementor sessionFactory) {
		return getExpressionText();
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public MappingModelExpressible getExpressionType() {
		return (MappingModelExpressible) jdbcMapping;
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
				readExpression
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
		return readExpression.equals( that.readExpression );
	}

	@Override
	public int hashCode() {
		return readExpression.hashCode();
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
