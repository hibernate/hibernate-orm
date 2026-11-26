package org.hibernate.sql.ast.spi.model;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jakarta.annotation.Nullable;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.InsertCoordinatorStandard;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;
import org.hibernate.sql.spi.mutation.jdbc.JdbcUpdateMutation;
import org.hibernate.sql.spi.mutation.jdbc.MergeOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;

import static org.hibernate.sql.ast.spi.model.AbstractTableUpdate.collectParameters;

/**
 * @author Steve Ebersole
 */
public class OptionalTableUpdate
		extends AbstractRestrictedTableMutation<MutationOperation>
		implements LogicalTableUpdate<MutationOperation> {
	private final List<ColumnValueBinding> valueBindings;
	private final boolean versionedTarget;
	private final @Nullable List<String> unassignedColumns;

	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			boolean versionedTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this(
				mutatingTable,
				mutationTarget,
				versionedTarget,
				true,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
	}

	/// @param assignsEveryColumn whether the assignments include every column of
	///                           the table, so that the row of an optional table
	///                           has no data exactly when every assigned value
	///                           is null
	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			boolean versionedTarget,
			boolean assignsEveryColumn,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this( mutatingTable, mutationTarget, versionedTarget,
				assignsEveryColumn ? insertOnlyColumns( valueBindings ) : null,
				valueBindings, keyRestrictionBindings, optLockRestrictionBindings );
	}

	/// @param unassignedColumns mapped state columns left untouched by the update,
	/// or null when the caller knows only that some columns are unassigned
	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			boolean versionedTarget,
			@Nullable List<String> unassignedColumns,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this(
				mutatingTable,
				mutationTarget,
				"upsert for " + mutationTarget.getRolePath(),
				versionedTarget,
				unassignedColumns,
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
	}

	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String comment,
			boolean versionedTarget,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		this(
				mutatingTable,
				mutationTarget,
				comment,
				versionedTarget,
				insertOnlyColumns( valueBindings ),
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
	}

	private OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			String comment,
			boolean versionedTarget,
			@Nullable List<String> unassignedColumns,
			List<ColumnValueBinding> valueBindings,
			List<ColumnValueBinding> keyRestrictionBindings,
			List<ColumnValueBinding> optLockRestrictionBindings) {
		super(
				mutatingTable,
				mutationTarget,
				comment,
				keyRestrictionBindings,
				optLockRestrictionBindings,
				collectParameters( valueBindings, keyRestrictionBindings, optLockRestrictionBindings )
		);
		this.valueBindings = valueBindings;
		this.versionedTarget = versionedTarget;
		if ( mutatingTable.isOptional() ) {
			this.unassignedColumns = unassignedColumns == null ? null : List.copyOf( unassignedColumns );
		}
		else {
			this.unassignedColumns = List.of();
		}
	}

	private static List<String> insertOnlyColumns(List<ColumnValueBinding> valueBindings) {
		return valueBindings.stream().filter( binding -> !binding.isAttributeUpdatable() )
				.map( binding -> binding.getColumnReference().getColumnExpression() ).toList();
	}

	@Override
	protected String getLoggableName() {
		return "OptionalTableUpdate";
	}

	@Override
	public boolean isCustomSql() {
		return false;
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	@Override
	public Expectation getExpectation() {
		return getMutatingTable().getTableMapping().getUpdateDetails().getExpectation();
	}

	@Override
	public void forEachParameter(Consumer<ColumnValueParameter> consumer) {
		final BiConsumer<Integer,ColumnValueBinding> intermediateConsumer = (index, binding) -> {
			for ( ColumnValueParameter parameter : binding.getValueExpression().getParameters() ) {
				consumer.accept( parameter );
			}
		};

		forEachThing( getValueBindings(), intermediateConsumer );
		forEachThing( getKeyBindings(), intermediateConsumer );
		forEachThing( getOptimisticLockBindings(), intermediateConsumer );
	}

	public List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	/// The target columns to test in the matched delete condition of a full MERGE.
	public List<String> getUnassignedColumns() {
		if ( unassignedColumns == null ) {
			throw new IllegalStateException( "Unassigned columns are unknown for " + getMutatingTable().getTableName() );
		}
		return unassignedColumns;
	}

	@Override
	public void forEachValueBinding(BiConsumer<Integer, ColumnValueBinding> consumer) {
		forEachThing( valueBindings, consumer );
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitOptionalTableUpdate( this );
	}

	@Override
	public MutationOperation createMutationOperation(ValuesAnalysis valuesAnalysis, SessionFactoryImplementor factory) {
		final TableMapping tableMapping = getMutatingTable().getTableMapping();
		assert ! (valuesAnalysis instanceof InsertCoordinatorStandard.InsertValuesAnalysis );
		if ( tableMapping.getInsertDetails().getCustomSql() != null
				|| tableMapping.getInsertDetails().isDynamicMutation()
				|| tableMapping.getDeleteDetails().getCustomSql() != null
				|| tableMapping.getUpdateDetails().getCustomSql() != null
				|| tableMapping.getUpdateDetails().isDynamicMutation()
				|| unassignedColumns == null ) {
			// Custom SQL and dynamic mutations need separate statements. A full MERGE
			// also needs the names of the columns whose stored values it must check.
			return new OptionalTableUpdateOperation( getMutationTarget(), this );
		}
		final var operation = factory.getJdbcServices().getDialect().createOptionalTableUpdateOperation(
				new OptionalTableUpdateOperationRequest( this, factory, versionedTarget )
		);
		// Only a full MERGE can refer to the existing row when deciding to delete it.
		return !unassignedColumns.isEmpty() && !(operation instanceof MergeOperation)
				? new OptionalTableUpdateOperation( getMutationTarget(), this )
				: operation;
	}

	@Override
	protected MutationOperation createMutationOperation(
			TableMapping tableDetails,
			String updateSql,
			List<JdbcParameterBinder> effectiveBinders) {
		return new JdbcUpdateMutation(
				tableDetails,
				getMutationTarget(),
				updateSql,
				isCallable(),
				getExpectation(),
				effectiveBinders
		);
	}
}
