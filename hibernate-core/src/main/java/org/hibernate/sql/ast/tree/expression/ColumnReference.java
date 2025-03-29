/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.expression;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.update.Assignable;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.internal.util.StringHelper.nullIfEmpty;
import static org.hibernate.internal.util.StringHelper.replace;
import static org.hibernate.sql.Template.TEMPLATE;

/**
 * Models a reference to a Column in a SQL AST
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 * @author Yanming Zhou
 */
public class ColumnReference implements Expression, Assignable {
	private final @Nullable String qualifier;
	private final String columnExpression;
	private final @Nullable SelectablePath selectablePath;
	private final boolean isFormula;
	private final @Nullable String readExpression;
	private final JdbcMapping jdbcMapping;

	public ColumnReference(TableReference tableReference, SelectableMapping selectableMapping) {
		this(
				tableReference.getIdentificationVariable(),
				selectableMapping.getSelectionExpression(),
				selectableMapping.getSelectablePath(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				selectableMapping.getJdbcMapping()
		);
	}

	public ColumnReference(TableReference tableReference, String mapping, JdbcMapping jdbcMapping) {
		this(
				tableReference.getIdentificationVariable(),
				mapping,
				null,
				false,
				null,
				jdbcMapping
		);
	}

	public ColumnReference(@Nullable String qualifier, SelectableMapping selectableMapping) {
		this(
				qualifier,
				selectableMapping.getSelectionExpression(),
				selectableMapping.getSelectablePath(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				selectableMapping.getJdbcMapping()
		);
	}

	public ColumnReference(@Nullable String qualifier, SelectableMapping selectableMapping, JdbcMapping jdbcMapping) {
		this(
				qualifier,
				selectableMapping.getSelectionExpression(),
				selectableMapping.getSelectablePath(),
				selectableMapping.isFormula(),
				selectableMapping.getCustomReadExpression(),
				jdbcMapping
		);
	}

	public ColumnReference(
			TableReference tableReference,
			String columnExpression,
			boolean isFormula,
			@Nullable String customReadExpression,
			JdbcMapping jdbcMapping) {
		this(
				tableReference.getIdentificationVariable(),
				columnExpression,
				null,
				isFormula,
				customReadExpression,
				jdbcMapping
		);
	}

	public ColumnReference(
			@Nullable String qualifier,
			String columnExpression,
			boolean isFormula,
			@Nullable String customReadExpression,
			JdbcMapping jdbcMapping) {
		this( qualifier, columnExpression, null, isFormula, customReadExpression, jdbcMapping );
	}

	public ColumnReference(
			@Nullable String qualifier,
			String columnExpression,
			@Nullable SelectablePath selectablePath,
			boolean isFormula,
			@Nullable String customReadExpression,
			JdbcMapping jdbcMapping) {
		this.qualifier = nullIfEmpty( qualifier );

		if ( isFormula ) {
			this.columnExpression = qualifier == null
					? replace( columnExpression, TEMPLATE + '.', "" )
					: replace( columnExpression, TEMPLATE, qualifier );
		}
		else {
			this.columnExpression = columnExpression;
		}

		this.selectablePath = selectablePath == null
				? new SelectablePath( this.columnExpression )
				: selectablePath;

		this.isFormula = isFormula;
		this.readExpression = customReadExpression;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public ColumnReference getColumnReference() {
		return this;
	}

	public @Nullable String getQualifier() {
		return qualifier;
	}

	public String getColumnExpression() {
		return columnExpression;
	}

	public @Nullable String getReadExpression() {
		return readExpression;
	}

	public @Nullable String getSelectableName() {
		return selectablePath == null ? null : selectablePath.getSelectableName();
	}

	public @Nullable SelectablePath getSelectablePath() {
		return selectablePath;
	}

	public boolean isColumnExpressionFormula() {
		return isFormula;
	}

	public String getExpressionText() {
		final StringBuilder sb = new StringBuilder();
		appendReadExpression( new StringBuilderSqlAppender( sb ) );
		return sb.toString();
	}

	public void appendReadExpression(SqlAppender appender) {
		appendReadExpression( appender, qualifier );
	}

	public void appendReadExpression(@Nullable String qualifier, Consumer<String> appender) {
		if ( isFormula ) {
			appender.accept( columnExpression );
		}
		else if ( readExpression != null ) {
			appender.accept( qualifier == null
					? replace( readExpression, TEMPLATE + '.', "" )
					: replace( readExpression, TEMPLATE, qualifier ) );
		}
		else {
			if ( qualifier != null ) {
				appender.accept( qualifier );
				appender.accept( "." );
			}
			appender.accept( columnExpression );
		}
	}

	public void appendReadExpression(SqlAppender appender, @Nullable String qualifier) {
		appendReadExpression( qualifier, appender::appendSql );
	}

	public void appendColumnForWrite(SqlAppender appender) {
		appendColumnForWrite( appender, qualifier );
	}

	public void appendColumnForWrite(SqlAppender appender, @Nullable String qualifier) {
		if ( qualifier != null ) {
			appender.append( qualifier );
			appender.append( '.' );
		}
		appender.append( columnExpression );
	}

	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	@Override
	public JdbcMapping getExpressionType() {
		return jdbcMapping;
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
				getExpressionText()
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
		return isFormula == that.isFormula
				&& Objects.equals( qualifier, that.qualifier )
				&& Objects.equals( columnExpression, that.columnExpression )
				&& Objects.equals( readExpression, that.readExpression );
	}

	@Override
	public int hashCode() {
		int result = qualifier != null ? qualifier.hashCode() : 0;
		result = 31 * result + ( columnExpression != null ? columnExpression.hashCode() : 0 );
		result = 31 * result + ( isFormula ? 1 : 0 );
		result = 31 * result + ( readExpression != null ? readExpression.hashCode() : 0 );
		return result;
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
