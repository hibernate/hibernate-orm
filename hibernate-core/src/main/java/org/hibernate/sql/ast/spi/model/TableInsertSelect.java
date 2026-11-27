package org.hibernate.sql.ast.spi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import jakarta.annotation.Nullable;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationTarget;

/// An insert which copies a row of a source table to the mutating table.
///
/// The key columns of the source table, and each [copied column][#getCopiedColumns],
/// are read from the source row and written to the columns with the same names.
/// Each [value binding][#getValueBindings] assigns a column which does not come
/// from the source row.
///
/// No row is inserted when no row of the source table has the given key. If the
/// source row is optional, an [anchor table][#getAnchorTableName] with the same
/// key decides instead whether a row is inserted. The key is then read from the
/// row of the anchor table, and the copied columns are null when the source row
/// does not exist.
///
/// The source table may instead hold several rows with the given key, ordered by
/// a [row order column][#getRowOrderColumn]. The last of them is then copied. Such
/// a source table may be the mutating table itself.
///
/// @author Gavin King
/// @since 8.0
public final class TableInsertSelect extends AbstractTableInsert {
	private final String sourceTableName;
	private final List<String> copiedColumns;
	private final List<ColumnValueBinding> keyRestrictions;
	private final @Nullable String anchorTableName;
	private final List<String> anchorKeyColumns;
	private final @Nullable String rowOrderColumn;

	/// @param copiedColumns the copied columns, not including the key columns
	/// @param keyRestrictions the restriction of each key column of the source table
	public TableInsertSelect(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sourceTableName,
			List<String> copiedColumns,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictions) {
		this( mutatingTable, mutationTarget, sourceTableName, copiedColumns, valueBindings, keyRestrictions,
				null, List.of(), null );
	}

	/// @param copiedColumns the copied columns, not including the key columns
	/// @param keyRestrictions the restriction of each key column of the source table
	/// @param anchorTableName the table whose row decides whether a row is inserted
	/// @param anchorKeyColumns the key columns of the anchor table, in the order of the restrictions
	public TableInsertSelect(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sourceTableName,
			List<String> copiedColumns,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictions,
			String anchorTableName,
			List<String> anchorKeyColumns) {
		this( mutatingTable, mutationTarget, sourceTableName, copiedColumns, valueBindings, keyRestrictions,
				anchorTableName, anchorKeyColumns, null );
		assert anchorKeyColumns.size() == keyRestrictions.size();
	}

	/// @param copiedColumns the copied columns, not including the key columns
	/// @param keyRestrictions the restriction of each key column of the source table
	/// @param rowOrderColumn the column which orders the rows of the source table having the given key
	public TableInsertSelect(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sourceTableName,
			List<String> copiedColumns,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictions,
			String rowOrderColumn) {
		this( mutatingTable, mutationTarget, sourceTableName, copiedColumns, valueBindings, keyRestrictions,
				null, List.of(), rowOrderColumn );
	}

	private TableInsertSelect(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sourceTableName,
			List<String> copiedColumns,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictions,
			@Nullable String anchorTableName,
			List<String> anchorKeyColumns,
			@Nullable String rowOrderColumn) {
		super( mutatingTable, mutationTarget, parameters( valueBindings, keyRestrictions ), valueBindings );
		this.sourceTableName = sourceTableName;
		this.copiedColumns = copiedColumns;
		this.keyRestrictions = keyRestrictions;
		this.anchorTableName = anchorTableName;
		this.anchorKeyColumns = anchorKeyColumns;
		this.rowOrderColumn = rowOrderColumn;
	}

	private static List<ColumnValueParameter> parameters(
			List<ColumnValueBinding> valueBindings, List<ColumnValueBinding> keyRestrictions) {
		final List<ColumnValueParameter> parameters = new ArrayList<>();
		for ( var binding : valueBindings ) {
			parameters.addAll( binding.getValueExpression().getParameters() );
		}
		for ( var restriction : keyRestrictions ) {
			parameters.addAll( restriction.getValueExpression().getParameters() );
		}
		return parameters;
	}

	/// The table from which the row is read.
	public String getSourceTableName() {
		return sourceTableName;
	}

	/// The names of the columns copied from the source row, not including its key columns.
	public List<String> getCopiedColumns() {
		return copiedColumns;
	}

	/// The restrictions on the key columns of the source table.
	public List<ColumnValueBinding> getKeyRestrictions() {
		return keyRestrictions;
	}

	/// The table whose row decides whether a row is inserted, if the source row is optional.
	public @Nullable String getAnchorTableName() {
		return anchorTableName;
	}

	/// The key columns of the anchor table, in the order of the [key restrictions][#getKeyRestrictions].
	public List<String> getAnchorKeyColumns() {
		return anchorKeyColumns;
	}

	/// The column which orders the rows of the source table having the given key,
	/// if it may hold several of them. The row with the greatest value is copied.
	public @Nullable String getRowOrderColumn() {
		return rowOrderColumn;
	}

	/// The source row need not exist.
	@Override
	public Expectation getExpectation() {
		return Expectation.None.INSTANCE;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return List.of();
	}

	@Override
	public void forEachReturningColumn(BiConsumer<Integer, ColumnReference> consumer) {
		// a copied row returns no column
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitTableInsertSelect( this );
	}
}
