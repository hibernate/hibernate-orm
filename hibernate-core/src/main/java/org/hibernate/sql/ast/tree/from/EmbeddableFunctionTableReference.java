/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;

import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * A table reference for functions that produce embeddable typed results.
 */
public class EmbeddableFunctionTableReference extends AbstractTableReference {

	private final String tableExpression;
	private final Expression expression;

	public EmbeddableFunctionTableReference(
			NavigablePath navigablePath,
			EmbeddableMappingType embeddableMappingType,
			Expression expression) {
		super( navigablePath.getFullPath(), false );
		this.tableExpression = embeddableMappingType.getAggregateMapping().getContainingTableExpression();
		this.expression = expression;
	}

	public Expression getExpression() {
		return expression;
	}

	@Override
	public boolean isEmbeddableFunctionTableReference() {
		return true;
	}

	@Override
	public EmbeddableFunctionTableReference asEmbeddableFunctionTableReference() {
		return this;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		expression.accept( sqlTreeWalker );
	}

	@Override
	public List<String> getAffectedTableNames() {
		return Collections.singletonList( tableExpression );
	}

	@Override
	public boolean containsAffectedTableName(String requestedName) {
		return isEmpty( requestedName ) || tableExpression.equals( requestedName );
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( tableExpression );
	}

	@Override
	public String getTableId() {
		return tableExpression;
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		return nameCollector.apply( tableExpression );
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression) {
		if ( this.tableExpression.equals( tableExpression ) ) {
			return this;
		}

		throw new UnknownTableReferenceException(
				tableExpression,
				String.format(
						Locale.ROOT,
						"Unable to determine TableReference (`%s`) for `%s`",
						tableExpression,
						navigablePath
				)
		);
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		return this.tableExpression.equals( tableExpression ) ? this : null;
	}

	@Override
	public String toString() {
		return identificationVariable;
	}
}
