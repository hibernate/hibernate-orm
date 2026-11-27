package org.hibernate.sql.ast.spi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationTarget;

/// An insert which copies a row of a source table to the mutating table.
///
/// Each [copied column][#getCopiedColumns] is read from the source row and
/// written to the column with the same name. Each [value binding][#getValueBindings]
/// assigns a column which does not come from the source row. No row is inserted
/// when no row of the source table satisfies the [restrictions][#getSourceRestrictions].
///
/// @author Gavin King
/// @since 8.0
public final class TableInsertSelect extends AbstractTableInsert {
	private final String sourceTableName;
	private final List<String> copiedColumns;
	private final List<ColumnValueBinding> sourceRestrictions;

	public TableInsertSelect(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sourceTableName,
			List<String> copiedColumns,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> sourceRestrictions) {
		super( mutatingTable, mutationTarget, parameters( valueBindings, sourceRestrictions ), valueBindings );
		this.sourceTableName = sourceTableName;
		this.copiedColumns = copiedColumns;
		this.sourceRestrictions = sourceRestrictions;
	}

	private static List<ColumnValueParameter> parameters(
			List<ColumnValueBinding> valueBindings, List<ColumnValueBinding> sourceRestrictions) {
		final List<ColumnValueParameter> parameters = new ArrayList<>();
		for ( var binding : valueBindings ) {
			parameters.addAll( binding.getValueExpression().getParameters() );
		}
		for ( var restriction : sourceRestrictions ) {
			parameters.addAll( restriction.getValueExpression().getParameters() );
		}
		return parameters;
	}

	/// The table from which the row is read.
	public String getSourceTableName() {
		return sourceTableName;
	}

	/// The names of the columns copied from the source row.
	public List<String> getCopiedColumns() {
		return copiedColumns;
	}

	/// The restrictions identifying the source row.
	public List<ColumnValueBinding> getSourceRestrictions() {
		return sourceRestrictions;
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
