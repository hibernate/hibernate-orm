/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.from.TableReference;

import java.util.function.Function;

/// Simple TableReference wrapper for {@link EntityTableDescriptor}.
///
/// Used internally by graph mutation builders to create ColumnReference instances
/// that are compatible with existing ColumnValueBinding infrastructure.
///
/// @author Steve Ebersole
@Incubating
public record MutatingTableReference(TableDescriptor tableDescriptor) implements TableReference {
	@Override
	public String getIdentificationVariable() {
		return null;
	}

	@Override
	public String getTableId() {
		return tableDescriptor.name();
	}

	@Override
	public boolean isOptional() {
		return tableDescriptor.isOptional();
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new UnsupportedOperationException(
				"SimpleTableReference is a builder-only type, not meant for SQL AST walking"
		);
	}

	@Override
	public Boolean visitAffectedTableNames(Function<String, Boolean> nameCollector) {
		return nameCollector.apply( tableDescriptor.name() );
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression) {
		if ( tableDescriptor.name().equals( tableExpression ) ||
			tableDescriptor.name().equals( tableExpression ) ) {
			return this;
		}
		throw new IllegalArgumentException(
				"Table expression (" + tableExpression + ") does not match this table: " +
				tableDescriptor.name()
		);
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression) {
		return resolveTableReference( navigablePath, tableExpression );
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean resolve) {
		if ( tableDescriptor.name().equals( tableExpression ) ||
			tableDescriptor.name().equals( tableExpression ) ) {
			return this;
		}
		return null;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			ValuedModelPart modelPart,
			String tableExpression,
			boolean resolve) {
		return getTableReference( navigablePath, tableExpression, resolve );
	}
}
