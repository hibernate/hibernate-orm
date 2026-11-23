package org.hibernate.sql.ast.spi.model;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.jdbc.JdbcInsertMutation;

/**
 * @author Steve Ebersole
 */
public class TableInsertStandard extends AbstractTableInsert implements TableInsert, GeneratedMutation<JdbcInsertMutation> {
	private final List<ColumnReference> returningColumns;
	private final boolean hasValueSubquery;

	public TableInsertStandard(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnReference> returningColumns,
			List<ColumnValueParameter> parameters) {
		this( mutatingTable, mutationTarget, valueBindings, returningColumns, parameters, false );
	}

	public TableInsertStandard(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnReference> returningColumns,
			List<ColumnValueParameter> parameters,
			boolean hasValueSubquery) {
		super( mutatingTable, mutationTarget, parameters, valueBindings );
		this.hasValueSubquery = hasValueSubquery;
		this.returningColumns = returningColumns;
	}

	/// Whether any inserted value is a scalar subquery.
	public boolean hasValueSubquery() {
		return hasValueSubquery;
	}

	@Override
	public boolean isCustomSql() {
		return false;
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return returningColumns;
	}

	@Override
	public void forEachReturningColumn(BiConsumer<Integer,ColumnReference> consumer) {
		forEachThing( returningColumns, consumer );
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitStandardTableInsert( this );
	}
}
