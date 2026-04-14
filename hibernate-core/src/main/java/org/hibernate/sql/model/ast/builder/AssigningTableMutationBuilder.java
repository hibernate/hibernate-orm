/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.TableMutation;

/// Specialized builder for building mutations which allow assigning values (INSERT or UPDATE).
///
/// @author Steve Ebersole
/// @author Gavin King
public interface AssigningTableMutationBuilder<M extends TableMutation<?>> extends TableMutationBuilder<M> {
	/// Whether this builder currently contains any assignment bindings.
	@Incubating
	boolean hasAssignmentBindings();

	/// Adds a column assignment defined by the given `columnValueBinding`, which represents a
	/// [column][ColumnValueBinding#columnReference()] and its [assignment][ColumnValueBinding#getValueExpression()] .
	@Incubating
	void addColumnAssignment(ColumnValueBinding columnValueBinding);

	/// Adds a column assignment defined by `columnMapping = {columnMapping.getWriteExpression()}`
	@Incubating
	default void addColumnAssignment(SelectableMapping columnMapping) {
		// Formulas are read-only computed columns and cannot be included in UPDATE/INSERT statements
		if ( !columnMapping.isFormula() ) {
			addColumnAssignment( columnMapping, columnMapping.getWriteExpression() );
		}
	}

	/// Adds a column assignment defined by `columnMapping = assignment`
	@Incubating
	void addColumnAssignment(SelectableMapping columnMapping, String assignment);

	/// Acts as a [org.hibernate.metamodel.mapping.SelectableConsumer].
	default void addColumnAssignment(int index, SelectableMapping selectableMapping) {
		addValueColumn(
				selectableMapping.getWriteExpression(),
				selectableMapping
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/// Adds a restriction, which is assumed to be based on a table key.
	///
	/// @apiNote Be sure you know what you are doing before using this method.  Generally
	/// prefer any of the other methods here for adding key restrictions.
	/// @deprecated Use [#addColumnAssignment(ColumnValueBinding)] instead.
	@Internal
	@Incubating
	@Deprecated(since = "8.0")
	default void addValueColumn(ColumnValueBinding valueBinding) {
		addColumnAssignment( valueBinding );
	}

	/// Add a column as part of the values list
	///
	/// @deprecated Use [#addColumnAssignment(SelectableMapping, String)] instead.
	@Deprecated(since = "8.0")
	default void addValueColumn(String columnWriteFragment, SelectableMapping selectableMapping) {
		addColumnAssignment(  selectableMapping, columnWriteFragment );
	}

	/// Add a column as part of the values list
	///
	/// @deprecated Use [#addColumnAssignment(SelectableMapping)] instead.
	@Deprecated(since = "8.0")
	default void addValueColumn(SelectableMapping selectableMapping) {
		if ( !selectableMapping.isFormula() ) {
//		if ( selectableMapping.isInsertable() || selectableMapping.isUpdateable() ) {
			addValueColumn(
					selectableMapping.getWriteExpression(),
					selectableMapping
			);
		}
	}

	/// Add a key column
	///
	/// @deprecated Use [#addColumnAssignment(int, SelectableMapping)] instead.
	@Deprecated(since = "8.0")
	default void addValueColumn(int index, SelectableMapping selectableMapping) {
		addValueColumn(
				selectableMapping.getWriteExpression(),
				selectableMapping
		);
	}

	/// @deprecated Use [#hasAssignmentBindings()] instead.
	@Deprecated(since = "8.0")
	default boolean hasValueBindings() {
		return hasAssignmentBindings();
	}
}
