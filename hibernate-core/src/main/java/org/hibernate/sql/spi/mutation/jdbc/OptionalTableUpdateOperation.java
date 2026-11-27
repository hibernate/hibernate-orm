package org.hibernate.sql.spi.mutation.jdbc;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import org.hibernate.SPI;
import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueDescriptorImpl;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.SelfExecutingUpdateOperation;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.sql.spi.mutation.ValuesAnalysis;
import org.hibernate.sql.ast.spi.model.AbstractTableUpdate;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.ast.spi.model.TableDeleteCustomSql;
import org.hibernate.sql.ast.spi.model.TableDeleteStandard;
import org.hibernate.sql.ast.spi.model.TableInsertCustomSql;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.ast.spi.model.TableUpdateCustomSql;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.ast.spi.model.TenantIdColumnValueBinding;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

/**
 * Legacy "upsert" handling, conditionally using INSERT, UPDATE and DELETE
 * statements as required for optional secondary tables.
 *
 * @author Steve Ebersole
 */
@SPI({ USE, IMPLEMENT }) // By Hibernate Reactive
public class OptionalTableUpdateOperation implements SelfExecutingUpdateOperation {
	private final MutationTarget mutationTarget;
	private final TableMapping tableMapping;
	private final Expectation expectation;

	private final List<ColumnValueBinding> valueBindings;
	private final List<ColumnValueBinding> keyBindings;
	private final List<ColumnValueBinding> optimisticLockBindings;
	private final boolean requiresExistingRowForOptimisticLock;
	private final List<ColumnValueParameter> parameters;
	private final List<ColumnValueParameter> deleteParameters;
	private final List<String> unassignedColumns;
	private final List<ColumnValueBinding> insertableStateBindings;

	private final Map<ParameterUsage, Map<String, JdbcValueDescriptor>> descriptorsByUsage =
			new EnumMap<>( ParameterUsage.class );
	private final int bindingCount;

	// Each statement depends only on the bindings, so it is translated once,
	// however many times the operation is performed.
	private volatile StatementPlan translatedUpdate;
	private volatile StatementPlan translatedUpdateWithStoredValues;
	private volatile StatementPlan translatedInsert;
	private volatile StatementPlan translatedDelete;

	public OptionalTableUpdateOperation(
			MutationTarget mutationTarget,
			OptionalTableUpdate upsert) {
		this( mutationTarget, upsert, upsert.getExpectation() );
	}

	@SPI(IMPLEMENT)
	protected OptionalTableUpdateOperation(
			MutationTarget mutationTarget,
			OptionalTableUpdate upsert,
			Expectation expectation) {
		this.mutationTarget = mutationTarget;
		this.tableMapping = upsert.getMutatingTable().getTableMapping();
		this.expectation = expectation;
		this.valueBindings = upsert.getValueBindings();
		this.keyBindings = upsert.getKeyBindings();
		this.optimisticLockBindings = upsert.getOptimisticLockBindings();
		this.requiresExistingRowForOptimisticLock = upsert.requiresExistingRowForOptimisticLock();
		this.parameters = upsert.getParameters();
		final var deleteDetails = tableMapping.getDeleteDetails();
		final boolean customDelete = deleteDetails != null && deleteDetails.getCustomSql() != null;
		this.deleteParameters = AbstractTableUpdate.collectParameters(
				emptyList(), keyBindings, customDelete ? emptyList() : optimisticLockBindings );
		// Custom DML supplies its own predicates.
		final var updateDetails = tableMapping.getUpdateDetails();
		this.unassignedColumns = !customDelete && (updateDetails == null || updateDetails.getCustomSql() == null)
				? upsert.getUnassignedColumns() : List.of();
		this.insertableStateBindings = unassignedColumns.isEmpty() ? List.of() : upsert.getInsertableStateBindings();

		int position = 0;
		for ( var parameter : parameters ) {
			registerDescriptor( parameter, ++position );
		}
		// Insert parameters keep their positions. The update additionally compares the old row.
		for ( var binding : valueBindings ) {
			if ( binding.isUpdateConditional() ) {
				for ( var parameter : binding.forUpdate( null ).getValueExpression().getParameters() ) {
					if ( parameter.getUsage() == ParameterUsage.RESTRICT ) {
						registerDescriptor( parameter, ++position );
					}
				}
			}
		}
		bindingCount = position;
	}

	private void registerDescriptor(ColumnValueParameter parameter, int position) {
		final var descriptor = new JdbcValueDescriptorImpl( parameter, position );
		// Repeated occurrences take their value from the first binding for this column and usage.
		descriptorsByUsage.computeIfAbsent( descriptor.getUsage(), usage -> new HashMap<>() )
				.putIfAbsent( descriptor.getColumnName(), descriptor );
	}

	@Override
	public final MutationType getMutationType() {
		// for Hibernate's purpose, an UPSERT *is an* UPDATE
		return MutationType.UPDATE;
	}

	@Override
	public final MutationTarget getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public final TableMapping getTableDetails() {
		return tableMapping;
	}

	protected final List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	protected final List<ColumnValueBinding> getKeyBindings() {
		return keyBindings;
	}

	protected final List<ColumnValueBinding> getOptimisticLockBindings() {
		return optimisticLockBindings;
	}

	protected final List<ColumnValueParameter> getParameters() {
		return parameters;
	}

	@Override
	public final JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		final var descriptors = descriptorsByUsage.get( usage );
		return descriptors == null ? null : descriptors.get( columnName );
	}


	@Override
	@SPI(IMPLEMENT) // By Hibernate Reactive: throw an exception if called in a reactive scenario
	public void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis incomingValuesAnalysis,
			SharedSessionContractImplementor session) {
		final var valuesAnalysis = (UpdateValuesAnalysis) incomingValuesAnalysis;
		if ( valuesAnalysis.getTablesNeedingUpdate().contains( tableMapping )
				|| valuesAnalysis.getTablesNeedingDynamicUpdate().contains( tableMapping ) ) {
			try {
				if ( !unassignedColumns.isEmpty()
						|| valuesAnalysis.getTablesWithNonNullValues().contains( tableMapping ) ) {
					performUpdateOrInsert( jdbcValueBindings, session, valuesAnalysis );
				}
				// all the new values for this table were null - possibly delete the row
				else if ( valuesAnalysis.getTablesWithPreviousNonNullValues().contains( tableMapping )
						&& !performDelete( jdbcValueBindings, session ) ) {
					checkOptimisticLock();
				}
			}
			finally {
				jdbcValueBindings.afterStatement( tableMapping );
			}
		}
	}

	private void performUpdateOrInsert(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session,
			UpdateValuesAnalysis valuesAnalysis) {
		final boolean needsUpdate = valuesAnalysis.getTablesWithPreviousNonNullValues().contains( tableMapping );
		final var bindings = jdbcValueBindings.getBindingGroup( tableMapping.getTableName() );
		final boolean needsStoredValues = needsUpdate && !unassignedColumns.isEmpty()
				&& valueBindings.stream().filter( ColumnValueBinding::isAttributeUpdatable )
						.noneMatch( binding -> hasValue( binding, bindings ) );
		if ( needsUpdate ) {
			// If the managed state is empty, try the guarded DELETE first. An
			// omitted column may have changed since the entity was loaded.
			final boolean deleteFirst = needsStoredValues && !session.isStateless()
					&& !valuesAnalysis.getTablesWithNonNullValues().contains( tableMapping );
			if ( deleteFirst && performDelete( jdbcValueBindings, session ) ) {
				return;
			}
			if ( performUpdate( jdbcValueBindings, session, needsStoredValues ) ) {
				return;
			}
			if ( needsStoredValues && !deleteFirst && performDelete( jdbcValueBindings, session ) ) {
				return;
			}
			checkOptimisticLock();
		}

		if ( !unassignedColumns.isEmpty()
				&& insertableStateBindings.stream().noneMatch( binding -> hasValue( binding, bindings ) ) ) {
			return;
		}
		performInsertWithRetry( jdbcValueBindings, session, needsUpdate, needsStoredValues );
	}

	private void checkOptimisticLock() {
		if ( requiresExistingRowForOptimisticLock ) {
			throw new StaleStateException( mutationTarget.getRolePath() );
		}
	}

	private void performInsertWithRetry(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session,
			boolean needsUpdate,
			boolean needsStoredValues) {
		MODEL_MUTATION_LOGGER.upsertUpdateNoRowsPerformingInsert( tableMapping.getTableName() );
		try {
			final boolean retryUpdate = performInsert( jdbcValueBindings, session );
			if ( needsUpdate && retryUpdate ) {
				// In a concurrent insert-or-update scenario, the insert can be skipped, so we need to retry the
				// update one last time to ensure we write the correct data for this transaction
				final boolean updated = performUpdate( jdbcValueBindings, session, needsStoredValues );
				if ( !updated ) {
					// The row might have been deleted again in the meantime,
					// so report a stale state instead of trying again
					throw new StaleStateException( mutationTarget.getRolePath() );
				}
			}
		}
		catch (ConstraintViolationException cve) {
			if ( cve.getKind() == UNIQUE ) {
				// Ignore primary key violation if the insert is composed of just the primary key
				// or if we skipped the UPDATE attempt because no columns were updatable
				if ( valueBindings.stream().anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
					if ( optimisticLockBindings.isEmpty() && !tableMapping.isOptional() ) {
						// In a concurrent insert-or-update scenario, the insert can fail, so we need to retry the
						// update one last time to ensure we write the correct data for this transaction
						final boolean updated = performUpdate( jdbcValueBindings, session, needsStoredValues );
						if ( !updated ) {
							// The row might have been deleted again in the meantime,
							// so report a stale state instead of trying again
							throw new StaleStateException( mutationTarget.getRolePath(), cve );
						}
					}
					else {
						// assume it was the primary key constraint which was violated,
						// due to a new version of the row existing in the database
						throw new StaleStateException( mutationTarget.getRolePath(), cve );
					}
				}
			}
			else {
				throw cve;
			}
		}
	}

	private static boolean hasValue(ColumnValueBinding binding, BindingGroup bindings) {
		final var expression = binding.getValueExpression();
		if ( expression == null ) {
			return false;
		}
		if ( expression.getParameters().isEmpty() ) {
			return !"null".equalsIgnoreCase( expression.getFragment() );
		}
		for ( var parameter : expression.getParameters() ) {
			if ( bindings.getBinding( parameter.getColumnReference().getColumnExpression(), parameter.getUsage() )
					.getValue() != null ) {
				return true;
			}
		}
		return false;
	}

	private boolean performDelete(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final var plan = translatedDelete( session );
		final var jdbcDelete = plan.operation();
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var deleteStatement = createStatementDetails( jdbcDelete, jdbcCoordinator );
		session.getJdbcServices().getSqlStatementLogger().logStatement( jdbcDelete.getSqlString() );
		session.getJdbcSessionContext().getStatementObserver().performingSql( jdbcDelete.getSqlString(), -1 );
		try {
			plan.bind( jdbcValueBindings.getBindingGroup( tableMapping.getTableName() ), deleteStatement, session );
			return jdbcCoordinator.getResultSetReturn().executeUpdate( deleteStatement, jdbcDelete.getSqlString() ) > 0;
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e, "Unable to bind parameter for optional table mutation", jdbcDelete.getSqlString() );
		}
		finally {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( deleteStatement );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	@SPI(USE) // By Hibernate Reactive
	protected JdbcDeleteMutation createJdbcDelete(SharedSessionContractImplementor session) {
		final var deleteDetails = tableMapping.getDeleteDetails();
		final var table = new MutatingTableReference( tableMapping );
		final var target = getMutationTarget();
		final String comment = "upsert delete for " + mutationTarget.getRolePath();
		final String where = unassignedColumns.isEmpty() ? null
				: unassignedColumns.stream().map( column -> column + " is null" ).collect( joining( " and " ) );
		final var tableDelete =
				deleteDetails != null && deleteDetails.getCustomSql() != null
						? new TableDeleteCustomSql( table, target, comment, keyBindings, optimisticLockBindings, deleteParameters )
						: new TableDeleteStandard( table, target, comment, keyBindings, optimisticLockBindings, deleteParameters, where );

		final var factory = session.getSessionFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<JdbcDeleteMutation>( factory, tableDelete ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private boolean performUpdate(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session,
			boolean needsStoredValues) {
		MODEL_MUTATION_LOGGER.performingUpdate( tableMapping.getTableName() );

		final var jdbcServices = session.getJdbcServices();
		final var plan = translatedUpdate( session, needsStoredValues );
		final var statementDetails =
				new PreparedStatementGroupSingleTable( plan.operation(), session )
						.resolvePreparedStatementDetails( tableMapping.getTableName() );
		final String sql = statementDetails.getSqlString();
		try {
			final var updateStatement = statementDetails.resolveStatement();
			jdbcServices.getSqlStatementLogger().logStatement( sql );
			session.getJdbcSessionContext().getStatementObserver().performingSql( sql, -1 );
			plan.bind( jdbcValueBindings.getBindingGroup( tableMapping.getTableName() ), updateStatement, session );
			final int rowCount =
					session.getJdbcCoordinator().getResultSetReturn()
							.executeUpdate( updateStatement, sql );
			if ( rowCount == 0 ) {
				return false;
			}
			else {
				expectation.verifyOutcome(
						rowCount,
						updateStatement,
						-1,
						sql
				);
				return true;
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to execute mutation PreparedStatement against table '" + tableMapping.getTableName() + "'",
					sql
			);
		}
		finally {
			statementDetails.releaseStatement( session );
		}
	}

	private StatementPlan translatedUpdate(SharedSessionContractImplementor session, boolean needsStoredValues) {
		var update = needsStoredValues ? translatedUpdateWithStoredValues : translatedUpdate;
		if ( update == null ) {
			final String where = needsStoredValues
					? unassignedColumns.stream().map( column -> column + " is not null" ).collect( joining( " or " ) )
					: null;
			update = createStatementPlan( createJdbcUpdate( session, where ) );
			if ( needsStoredValues ) {
				translatedUpdateWithStoredValues = update;
			}
			else {
				translatedUpdate = update;
			}
		}
		return update;
	}

	private record StatementPlan(JdbcMutationOperation operation, int[][] jdbcPositions) {
		void bind(BindingGroup group, PreparedStatement statement, SharedSessionContractImplementor session)
				throws SQLException {
			int bound = 0;
			if ( group != null ) {
				for ( var binding : group.getBindings() ) {
					final int position = binding.getPosition();
					if ( jdbcPositions == null ) {
						if ( position <= operation.getParameterBinders().size() ) {
							binding.getValueBinder().bind( statement, binding.getValue(), position, session );
							bound++;
						}
						continue;
					}
					final int[] destinations = jdbcPositions[position];
					if ( destinations != null ) {
						for ( int destination : destinations ) {
							binding.getValueBinder().bind( statement, binding.getValue(), destination, session );
							bound++;
						}
					}
				}
			}
			if ( jdbcPositions != null && bound != operation.getParameterBinders().size() ) {
				throw new IllegalStateException( "Missing parameter binding for " + operation.getSqlString() );
			}
		}
	}

	private StatementPlan translatedInsert(SharedSessionContractImplementor session) {
		var insert = translatedInsert;
		if ( insert == null ) {
			final var operation = createJdbcOptionalInsert( session );
			translatedInsert = insert = valueBindings.stream().allMatch( ColumnValueBinding::isAttributeInsertable )
					? new StatementPlan( operation, null )
					: createStatementPlan( operation, 0 );
		}
		return insert;
	}

	private StatementPlan createStatementPlan(JdbcMutationOperation operation) {
		return createStatementPlan( operation, operation.getExpectation().getNumberOfParametersUsed() );
	}

	// Index the translated placeholders by the position of their value in the table's binding group.
	private StatementPlan createStatementPlan(JdbcMutationOperation operation, int parameterOffset) {
		final int[][] positions = new int[bindingCount + 1][];
		int destination = parameterOffset;
		for ( var binder : operation.getParameterBinders() ) {
			final var parameter = (ColumnValueParameter) binder;
			final String column = parameter.getColumnReference().getColumnExpression();
			final var descriptor = findValueDescriptor( column, parameter.getUsage() );
			if ( descriptor == null ) {
				throw new IllegalStateException( "Missing value descriptor for " + column + " in " + operation.getSqlString() );
			}
			final int source = descriptor.getJdbcPosition();
			// A dialect may render the same parameter more than once.
			final int[] previous = positions[source];
			final int length = previous == null ? 0 : previous.length;
			final int[] destinations = previous == null ? new int[1] : java.util.Arrays.copyOf( previous, length + 1 );
			destinations[length] = ++destination;
			positions[source] = destinations;
		}
		return new StatementPlan( operation, positions );
	}

	private StatementPlan translatedDelete(SharedSessionContractImplementor session) {
		var delete = translatedDelete;
		if ( delete == null ) {
			translatedDelete = delete = createStatementPlan( createJdbcDelete( session ) );
		}
		return delete;
	}

	@SPI(USE) // by Hibernate Reactive
	protected JdbcMutationOperation createJdbcUpdate(SharedSessionContractImplementor session) {
		return createJdbcUpdate( session, null );
	}

	private JdbcMutationOperation createJdbcUpdate(
			SharedSessionContractImplementor session,
			String whereFragment) {
		final var updateValueBindings = getUpdatableValueBindings();
		final var updateParameters = AbstractTableUpdate.collectParameters(
				updateValueBindings, keyBindings, optimisticLockBindings );
		final var updateDetails = tableMapping.getUpdateDetails();
		final String comment = "upsert update for " + mutationTarget.getRolePath();
		final var table = new MutatingTableReference( tableMapping );
		final var target = getMutationTarget();
		final var tableUpdate =
				updateDetails != null && updateDetails.getCustomSql() != null
						? new TableUpdateCustomSql( table, target, comment, updateValueBindings,
								keyBindings, optimisticLockBindings, updateParameters )
						: new TableUpdateStandard( table, target, comment, updateValueBindings,
								keyBindings, optimisticLockBindings, updateParameters, whereFragment, null );

		return session.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<>( session.getFactory(), tableUpdate ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private boolean performInsert(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final var plan = translatedInsert( session );
		final var jdbcInsert = plan.operation();
		final var jdbcServices = session.getJdbcServices();
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var insertStatement = createStatementDetails( jdbcInsert, jdbcCoordinator );
		final String sql = jdbcInsert.getSqlString();
		try {
			jdbcServices.getSqlStatementLogger().logStatement( sql );
			session.getJdbcSessionContext().getStatementObserver().performingSql( sql, -1 );
			plan.bind( jdbcValueBindings.getBindingGroup( tableMapping.getTableName() ), insertStatement, session );
			final int rowCount = jdbcCoordinator.getResultSetReturn().executeUpdate( insertStatement, sql );
			if ( rowCount == 0 && isRetryUpdateAfterInsert() ) {
				return true;
			}
			else {
				expectation.verifyOutcome(
						rowCount,
						insertStatement,
						-1,
						sql
				);
				return false;
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to execute mutation PreparedStatement against table '" + tableMapping.getTableName() + "'",
					sql
			);
		}
		finally {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( insertStatement );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	protected boolean isRetryUpdateAfterInsert() {
		return false;
	}

	@SPI(IMPLEMENT) // By Hibernate Reactive
	protected JdbcMutationOperation createJdbcOptionalInsert(SharedSessionContractImplementor session) {
		return createJdbcInsert( session );
	}

	@SPI(USE)// By Hibernate Reactive
	protected JdbcInsertMutation createJdbcInsert(SharedSessionContractImplementor session) {
		final var insertDetails = tableMapping.getInsertDetails();
		final var table = new MutatingTableReference( tableMapping );
		final var combinedBindings = getInsertBindings();
		final var target = getMutationTarget();
		final var tableInsert =
				insertDetails != null && insertDetails.getCustomSql() != null
						? new TableInsertCustomSql( table, target, combinedBindings, parameters )
						: new TableInsertStandard( table, target, combinedBindings, emptyList(), parameters );

		final var factory = session.getSessionFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.ModelMutation<JdbcInsertMutation>( factory, tableInsert ) )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private static PreparedStatement createStatementDetails(
			PreparableMutationOperation operation,
			JdbcCoordinator jdbcCoordinator) {
		final var statementPreparer = jdbcCoordinator.getMutationStatementPreparer();
		final var statement = statementPreparer.prepareStatement( operation.getSqlString(), false );
		jdbcCoordinator.getLogicalConnection().getResourceRegistry().register( null, statement );
		return statement;
	}

	@Override
	public String toString() {
		return "OptionalTableUpdateOperation(" + tableMapping + ")";
	}

	protected List<ColumnValueBinding> getInsertBindings() {
		final var insertBindings = new ArrayList<ColumnValueBinding>( valueBindings.size() + keyBindings.size() );
		for ( var binding : valueBindings ) {
			if ( binding.isAttributeInsertable() ) {
				insertBindings.add( binding );
			}
		}
		insertBindings.addAll( keyBindings );
		return insertBindings;
	}

	private List<ColumnValueBinding> getUpdatableValueBindings() {
		// when there is nothing to assign, the update still tells us whether the row exists
		if ( !valueBindings.isEmpty() && !needsSeparateUpdateBindings() ) {
			return valueBindings;
		}
		else {
			final List<ColumnValueBinding> updateBindings = arrayList( valueBindings.size() );
			for ( var binding : valueBindings ) {
				if ( binding.isAttributeUpdatable() ) {
					updateBindings.add( binding.forUpdate( null ) );
				}
			}
			if ( updateBindings.isEmpty() ) {
				final var noOpBinding = buildNoOpBinding();
				if ( noOpBinding != null ) {
					updateBindings.add( noOpBinding );
				}
			}
			return updateBindings;
		}
	}

	private boolean needsSeparateUpdateBindings() {
		for ( var binding : valueBindings ) {
			if ( !binding.isAttributeUpdatable() || binding.isUpdateConditional() ) {
				return true;
			}
		}
		return false;
	}

	private ColumnValueBinding buildNoOpBinding() {
		final var referenceBinding = getReferenceBinding();
		if ( referenceBinding != null ) {
			final var referenceExpression = referenceBinding.getValueExpression();
			if ( referenceExpression != null
					&& referenceExpression.getSqlTypedMapping()
							instanceof SelectableMapping selectableMapping ) {
				final var columnReference = referenceBinding.getColumnReference();
				return new ColumnValueBinding(
						columnReference,
						new ColumnWriteFragment(
								columnReference.getColumnExpression(),
								selectableMapping
						)
				);
			}
		}
		return null;
	}

	private ColumnValueBinding getReferenceBinding() {
		for ( var binding : optimisticLockBindings ) {
			if ( binding instanceof TenantIdColumnValueBinding ) {
				// Spanner rejects assignments to primary keys, even key=key.
				return binding;
			}
		}
		if ( !keyBindings.isEmpty() ) {
			return keyBindings.get( 0 );
		}
		else if ( !valueBindings.isEmpty() ) {
			return valueBindings.get( 0 );
		}
		else {
			return null;
		}
	}
}
