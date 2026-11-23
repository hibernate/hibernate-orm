package org.hibernate.sql.ast.spi.model;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.jdbc.JdbcMutationOperation;

/**
 * @author Steve Ebersole
 */
public class TableUpdateStandard
		extends AbstractTableUpdate<JdbcMutationOperation>
		implements GeneratedMutation<JdbcMutationOperation> {
	private final Predicate restriction;
	private final String whereFragment;
	private final Expectation expectation;
	private final List<ColumnReference> returningColumns;

	public TableUpdateStandard(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this(
				mutatingTable,
				mutationTarget,
				sqlComment,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				null,
				null,
				Collections.emptyList()
		);
	}

	public TableUpdateStandard(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			String whereFragment,
			Expectation expectation,
			List<ColumnReference> returningColumns) {
		super( mutatingTable, mutationTarget, sqlComment, valueBindings, keyRestrictionBindings, optLockRestrictionBindings );
		this.whereFragment = whereFragment;
		this.expectation = expectation;
		this.restriction = null;
		this.returningColumns = returningColumns;
	}

	public TableUpdateStandard(
			MutatingTableReference tableReference,
			MutationTarget mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters) {
		this(
				tableReference,
				mutationTarget,
				sqlComment,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				parameters,
				null,
				null
		);
	}

	public TableUpdateStandard(
			MutatingTableReference tableReference,
			MutationTarget mutationTarget,
			String sqlComment,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings,
			List<ColumnValueParameter> parameters,
			String whereFragment,
			Expectation expectation) {
		super( tableReference, mutationTarget, sqlComment, valueBindings, keyRestrictionBindings, optLockRestrictionBindings, parameters );
		this.whereFragment = whereFragment;
		this.expectation = expectation;
		this.restriction = null;
		this.returningColumns = Collections.emptyList();
	}

	public TableUpdateStandard(TableUpdateStandard original, List<ColumnValueBinding> values,
			List<ColumnValueBinding> keys, Predicate restriction, List<ColumnValueParameter> restrictionParameters) {
		super( original.getMutatingTable(), original.getMutationTarget(), original.getMutationComment(),
				values, keys, original.getOptimisticLockBindings() );
		this.whereFragment = original.whereFragment;
		this.expectation = original.expectation;
		this.returningColumns = original.returningColumns;
		this.restriction = restriction;
		getParameters().addAll( restrictionParameters );
	}

	@Override
	public void forEachParameter(Consumer<ColumnValueParameter> consumer) {
		if ( restriction == null ) {
			super.forEachParameter( consumer );
		}
		else {
			getParameters().forEach( consumer );
		}
	}

	@Override
	public boolean requiresMatchedRow() {
		return restriction != null;
	}

	public Predicate getAdditionalRestriction() {
		return restriction;
	}

	@Override
	public boolean isCustomSql() {
		return false;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	public String getWhereFragment() {
		return whereFragment;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitStandardTableUpdate( this );
	}

	@Override
	public Expectation getExpectation() {
		if ( expectation != null ) {
			return expectation;
		}
		return super.getExpectation();
	}

	@Override
	public List<ColumnReference> getReturningColumns() {
		return returningColumns;
	}

	@Override
	public void forEachReturningColumn(BiConsumer<Integer,ColumnReference> consumer) {
		forEachThing( returningColumns, consumer );
	}
}
