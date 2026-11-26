package org.hibernate.sql.ast.spi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
	private final boolean requiresExistingRowForOptimisticLock;
	private final List<String> unassignedColumns;

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
				insertOnlyColumns( valueBindings ),
				valueBindings,
				keyRestrictionBindings,
				optLockRestrictionBindings
		);
	}

	/// @param unassignedColumns mapped state columns left untouched by the update
	public OptionalTableUpdate(
			MutatingTableReference mutatingTable,
			MutationTarget mutationTarget,
			boolean versionedTarget,
			List<String> unassignedColumns,
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
			List<String> unassignedColumns,
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
		this.requiresExistingRowForOptimisticLock = mutatingTable.isOptional() && !versionedTarget
				&& optLockRestrictionBindings instanceof ColumnValueBindingList bindings
				&& bindings.hasNonNullOldValue();
		this.unassignedColumns = mutatingTable.isOptional() ? List.copyOf( unassignedColumns ) : List.of();
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

	/// A non-null old column value in a DIRTY or ALL restriction proves the row existed.
	public boolean requiresExistingRowForOptimisticLock() {
		return requiresExistingRowForOptimisticLock;
	}

	/// The columns whose stored values contribute to the state of the updated row.
	public List<String> getUnassignedColumns() {
		return unassignedColumns;
	}

	/// The bindings that can supply state to a newly inserted optional row.
	public List<ColumnValueBinding> getInsertableStateBindings() {
		final List<ColumnValueBinding> bindings = new ArrayList<>();
		for ( var binding : valueBindings ) {
			if ( binding.isAttributeInsertable() ) {
				bindings.add( binding );
			}
		}
		for ( var binding : getKeyBindings() ) {
			// A partition key may supply mapped state through its key binding.
			if ( unassignedColumns.contains( binding.getColumnReference().getColumnExpression() ) ) {
				bindings.add( binding );
			}
		}
		return bindings;
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
				|| tableMapping.getUpdateDetails().isDynamicMutation() ) {
			return new OptionalTableUpdateOperation( getMutationTarget(), this );
		}
		return factory.getJdbcServices().getDialect().createOptionalTableUpdateOperation(
				new OptionalTableUpdateOperationRequest( this, factory, versionedTarget )
		);
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
