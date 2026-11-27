package org.hibernate.sql.ast.spi.model;

import java.util.List;

import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.TableMapping;

/// An insert selecting values from a single row, identified by the source table's key.
public final class TableInsertSelect extends TableInsertStandard {
	private final TableMapping sourceTable;
	private final List<ColumnValueParameter> sourceKey;

	public TableInsertSelect(MutatingTableReference targetTable, MutationTarget target,
			List<ColumnValueBinding> columns, List<ColumnValueParameter> parameters,
			TableMapping sourceTable, List<ColumnValueParameter> sourceKey) {
		super( targetTable, target, columns, List.of(), parameters );
		this.sourceTable = sourceTable;
		this.sourceKey = sourceKey;
	}

	public TableMapping getSourceTable() {
		return sourceTable;
	}

	public List<ColumnValueParameter> getSourceKey() {
		return sourceKey;
	}
}
