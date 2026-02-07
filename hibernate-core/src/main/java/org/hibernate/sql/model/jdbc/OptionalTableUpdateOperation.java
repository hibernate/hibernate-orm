/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.StaleStateException;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueDescriptorImpl;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.AbstractTableUpdate;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
import org.hibernate.sql.model.ast.ColumnWriteFragment;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableDelete;
import org.hibernate.sql.model.ast.TableInsert;
import org.hibernate.sql.model.ast.TableUpdate;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.internal.TableDeleteCustomSql;
import org.hibernate.sql.model.internal.TableDeleteStandard;
import org.hibernate.sql.model.internal.TableInsertCustomSql;
import org.hibernate.sql.model.internal.TableInsertStandard;
import org.hibernate.sql.model.internal.TableUpdateCustomSql;
import org.hibernate.sql.model.internal.TableUpdateStandard;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.hibernate.exception.ConstraintViolationException.ConstraintKind.UNIQUE;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.sql.model.ModelMutationLogging.MODEL_MUTATION_LOGGER;

/**
 * Legacy "upsert" handling, conditionally using INSERT, UPDATE and DELETE
 * statements as required for optional secondary tables.
 *
 * @author Steve Ebersole
 */
public class OptionalTableUpdateOperation implements SelfExecutingUpdateOperation {
	private final EntityMutationTarget mutationTarget;
	private final EntityTableMapping tableMapping;
	private final Expectation expectation;

	private final List<ColumnValueBinding> valueBindings;
	private final List<ColumnValueBinding> keyBindings;
	private final List<ColumnValueBinding> optimisticLockBindings;
	private final List<ColumnValueParameter> parameters;

	private final List<JdbcValueDescriptor> jdbcValueDescriptors;

	public OptionalTableUpdateOperation(
			MutationTarget<?> mutationTarget,
			OptionalTableUpdate upsert,
			@SuppressWarnings("unused") SessionFactoryImplementor factory) {
		this.mutationTarget = (EntityMutationTarget) mutationTarget;
		this.tableMapping = (EntityTableMapping) upsert.getMutatingTable().getTableMapping();
		this.expectation = upsert.getExpectation();
		this.valueBindings = upsert.getValueBindings();
		this.keyBindings = upsert.getKeyBindings();
		this.optimisticLockBindings = upsert.getOptimisticLockBindings();
		this.parameters = upsert.getParameters();

		this.jdbcValueDescriptors = arrayList( parameters.size() );
		for ( int i = 0; i < parameters.size(); i++ ) {
			jdbcValueDescriptors.add( new JdbcValueDescriptorImpl( parameters.get( i ), i + 1 ) );
		}
	}

	@Override
	public MutationType getMutationType() {
		// for Hibernate's purpose, an UPSERT *is an* UPDATE
		return MutationType.UPDATE;
	}

	@Override
	public MutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	@Override
	public TableMapping getTableDetails() {
		return tableMapping;
	}

	public List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}

	public List<ColumnValueBinding> getKeyBindings() {
		return keyBindings;
	}

	public List<ColumnValueBinding> getOptimisticLockBindings() {
		return optimisticLockBindings;
	}

	public List<ColumnValueParameter> getParameters() {
		return parameters;
	}

	@Override
	public JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		for ( int i = 0; i < jdbcValueDescriptors.size(); i++ ) {
			final var descriptor = jdbcValueDescriptors.get( i );
			if ( descriptor.getColumnName().equals( columnName )
					&& descriptor.getUsage() == usage ) {
				return descriptor;
			}
		}
		return null;
	}

	@Override
	public void performMutation(
			JdbcValueBindings jdbcValueBindings,
			ValuesAnalysis incomingValuesAnalysis,
			SharedSessionContractImplementor session) {
		final var valuesAnalysis = (UpdateValuesAnalysis) incomingValuesAnalysis;
		if ( valuesAnalysis.getTablesNeedingUpdate().contains( tableMapping )
				|| valuesAnalysis.getTablesNeedingDynamicUpdate().contains( tableMapping ) ) {
			try {
				if ( valuesAnalysis.getTablesWithNonNullValues().contains( tableMapping ) ) {
					performUpdateOrInsert( jdbcValueBindings, session, valuesAnalysis );
				}
				// all the new values for this table were null - possibly delete the row
				else if ( valuesAnalysis.getTablesWithPreviousNonNullValues().contains( tableMapping ) ) {
					performDelete( jdbcValueBindings, session );
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
		// there are some non-null values for the table - we need to update or insert the values.
		// first, try the update and see if any row was affected
		final boolean wasUpdated;
		if ( valuesAnalysis.getTablesWithPreviousNonNullValues().contains( tableMapping ) ) {
			// either
			// 		1) not know if the values for this table were previously all null (because old values are not known)
			//		2) the values for this table were previously had at least one non-null
			wasUpdated = performUpdate( jdbcValueBindings, session );
		}
		else {
			wasUpdated = false;
		}

		if ( !wasUpdated ) {
			MODEL_MUTATION_LOGGER.upsertUpdateNoRowsPerformingInsert( tableMapping.getTableName() );
			try {
				performInsert( jdbcValueBindings, session );
			}
			catch (ConstraintViolationException cve) {
				if ( cve.getKind() == UNIQUE ) {
					// Ignore primary key violation if the insert is composed of just the primary key
					// or if we skipped the UPDATE attempt because no columns were updatable
					if ( valueBindings.stream().anyMatch( ColumnValueBinding::isAttributeUpdatable ) ) {
						// assume it was the primary key constraint which was violated,
						// due to a new version of the row existing in the database
						throw new StaleStateException( mutationTarget.getRolePath(), cve );
					}
				}
				else {
					throw cve;
				}
			}
		}
	}

	private void performDelete(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final var jdbcDelete = createJdbcDelete( session );
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var deleteStatement = createStatementDetails( jdbcDelete, jdbcCoordinator );
		session.getJdbcServices().getSqlStatementLogger().logStatement( jdbcDelete.getSqlString() );
		bindKeyValues( jdbcValueBindings, deleteStatement, jdbcDelete, session );
		try {
			session.getJdbcCoordinator().getResultSetReturn()
					.executeUpdate( deleteStatement, jdbcDelete.getSqlString() );
		}
		finally {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( deleteStatement );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	private void bindKeyValues(
			JdbcValueBindings jdbcValueBindings,
			PreparedStatement statement,
			JdbcDeleteMutation jdbcDelete,
			SharedSessionContractImplementor session) {
		final var bindingGroup = jdbcValueBindings.getBindingGroup( tableMapping.getTableName() );
		if ( bindingGroup == null ) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"No value bindings for table on insert : %s",
							tableMapping.getTableName()
					)
			);
		}

		int jdbcBindingPosition = 1;
		boolean foundKeyBindings = false;
		// leverage the fact that bindings are contiguous to avoid full nested iterations
		final var keyBindingsItr = keyBindings.iterator();
		bindings: for ( var binding : bindingGroup.getBindings() ) {
			// binding-position here is 1-based (JDBC)
			final var valueDescriptor = jdbcValueDescriptors.get( binding.getPosition() - 1 );
			// key bindings would have a usage of RESTRICT relative to the UPDATE
			if ( valueDescriptor.getUsage() == ParameterUsage.RESTRICT ) {
				while ( keyBindingsItr.hasNext() ) {
					if ( Objects.equals( keyBindingsItr.next().getColumnReference().getColumnExpression(),
							binding.getColumnName() ) ) {
						// binding is for a key column
						foundKeyBindings = true;
						bindKeyValue(
								jdbcBindingPosition++,
								binding,
								valueDescriptor,
								statement,
								jdbcDelete.getSqlString(),
								tableMapping,
								session
						);
						break;
					}
					else {
						if ( foundKeyBindings ) {
							// we are now "beyond" the key bindings
							break bindings;
						}
					}
				}
			}
		}
	}

	private static void bindKeyValue(
			int jdbcPosition,
			Binding binding,
			JdbcValueDescriptor valueDescriptor,
			PreparedStatement statement,
			String sql,
			EntityTableMapping tableMapping,
			SharedSessionContractImplementor session) {
		try {
			binding.getValueBinder().bind( statement, binding.getValue(), jdbcPosition, session );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					String.format(
							Locale.ROOT,
							"Unable to bind parameter for upsert insert : %s.%s",
							tableMapping.getTableName(),
							valueDescriptor.getColumnName()
					),
					sql
			);
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected JdbcDeleteMutation createJdbcDelete(SharedSessionContractImplementor session) {
		final TableDelete tableDelete;
		if ( tableMapping.getDeleteDetails() != null
				&& tableMapping.getDeleteDetails().getCustomSql() != null ) {
			tableDelete = new TableDeleteCustomSql(
					new MutatingTableReference( tableMapping ),
					getMutationTarget(),
					"upsert delete for " + mutationTarget.getRolePath(),
					keyBindings,
					optimisticLockBindings,
					parameters
			);
		}
		else {
			tableDelete = new TableDeleteStandard(
					new MutatingTableReference( tableMapping ),
					getMutationTarget(),
					"upsert delete for " + mutationTarget.getRolePath(),
					keyBindings,
					optimisticLockBindings,
					parameters
			);
		}

		final var factory = session.getSessionFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( tableDelete, factory )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private boolean performUpdate(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		MODEL_MUTATION_LOGGER.performingUpdate( tableMapping.getTableName() );

		final var jdbcServices = session.getJdbcServices();
		final var updateValueBindings = getUpdatableValueBindings();
		final var updateParameters = collectUpdateParameters( updateValueBindings );
		final var statementDetails =
				new PreparedStatementGroupSingleTable(
						createJdbcUpdate( session, updateValueBindings, updateParameters ),
						session
				)
						.resolvePreparedStatementDetails( tableMapping.getTableName() );
		final String sql = statementDetails.getSqlString();
		try {
			final var updateStatement = statementDetails.resolveStatement();
			jdbcServices.getSqlStatementLogger().logStatement( sql );
			bindUpdateValues( jdbcValueBindings, updateStatement, updateParameters, sql, session );
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

	/*
	 * Used by Hibernate Reactive
	 */
	protected JdbcMutationOperation createJdbcUpdate(SharedSessionContractImplementor session) {
		final var updateValueBindings = getUpdatableValueBindings();
		final var updateParameters = collectUpdateParameters( updateValueBindings );
		return createJdbcUpdate( session, updateValueBindings, updateParameters );
	}

	private JdbcMutationOperation createJdbcUpdate(
			SharedSessionContractImplementor session,
			List<ColumnValueBinding> updateValueBindings,
			List<ColumnValueParameter> updateParameters) {
		final TableUpdate<JdbcMutationOperation> tableUpdate;
		if ( tableMapping.getUpdateDetails() != null
				&& tableMapping.getUpdateDetails().getCustomSql() != null ) {
			tableUpdate = new TableUpdateCustomSql(
					new MutatingTableReference( tableMapping ),
					mutationTarget,
					"upsert update for " + mutationTarget.getRolePath(),
					updateValueBindings,
					keyBindings,
					optimisticLockBindings,
					updateParameters
			);
		}
		else {
			tableUpdate = new TableUpdateStandard(
					new MutatingTableReference( tableMapping ),
					mutationTarget,
					"upsert update for " + mutationTarget.getRolePath(),
					updateValueBindings,
					keyBindings,
					optimisticLockBindings,
					updateParameters
			);
		}

		return session.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( tableUpdate, session.getFactory() )
				.translate( null, MutationQueryOptions.INSTANCE );
	}

	private void performInsert(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final var jdbcInsert = createJdbcOptionalInsert( session );
		final var jdbcServices = session.getJdbcServices();
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var insertStatement = createStatementDetails( jdbcInsert, jdbcCoordinator );
		final String sql = jdbcInsert.getSqlString();
		try {
			jdbcServices.getSqlStatementLogger().logStatement( sql );
			final var bindingGroup = jdbcValueBindings.getBindingGroup( tableMapping.getTableName() );
			if ( bindingGroup != null ) {
				bindingGroup.forEachBinding( binding -> {
					// Skip parameter bindings for e.g. optimistic version check
					if ( binding.getPosition() <= jdbcInsert.getParameterBinders().size() ) {
						try {
							binding.getValueBinder().bind(
									insertStatement,
									binding.getValue(),
									binding.getPosition(),
									session
							);
						}
						catch (SQLException e) {
							throw jdbcServices.getSqlExceptionHelper().convert(
									e,
									"Unable to bind parameter for upsert insert",
									sql
							);
						}
					}
				} );
			}
			jdbcCoordinator.getResultSetReturn()
					.executeUpdate( insertStatement, sql );
		}
		finally {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( insertStatement );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected JdbcMutationOperation createJdbcOptionalInsert(SharedSessionContractImplementor session) {
		return createJdbcInsert( session );
	}

	/*
	 * Used by Hibernate Reactive
	 */
	protected JdbcInsertMutation createJdbcInsert(SharedSessionContractImplementor session) {
		final TableInsert tableInsert;
		if ( tableMapping.getInsertDetails() != null
				&& tableMapping.getInsertDetails().getCustomSql() != null ) {
			tableInsert = new TableInsertCustomSql(
					new MutatingTableReference( tableMapping ),
					getMutationTarget(),
					CollectionHelper.combine( valueBindings, keyBindings ),
					parameters
			);
		}
		else {
			tableInsert = new TableInsertStandard(
					new MutatingTableReference( tableMapping ),
					getMutationTarget(),
					CollectionHelper.combine( valueBindings, keyBindings ),
					Collections.emptyList(),
					parameters
			);
		}

		final var factory = session.getSessionFactory();
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( tableInsert, factory )
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

	private List<ColumnValueBinding> getUpdatableValueBindings() {
		if ( !hasNonUpdatableBinding() ) {
			return valueBindings;
		}
		else {
			final List<ColumnValueBinding> updateBindings = arrayList( valueBindings.size() );
			for ( var binding : valueBindings ) {
				if ( binding.isAttributeUpdatable() ) {
					updateBindings.add( binding );
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

	private boolean hasNonUpdatableBinding() {
		for ( var binding : valueBindings ) {
			if ( !binding.isAttributeUpdatable() ) {
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

	private List<ColumnValueParameter> collectUpdateParameters(
			List<ColumnValueBinding> updateValueBindings) {
		return AbstractTableUpdate.collectParameters(
				updateValueBindings,
				keyBindings,
				optimisticLockBindings
		);
	}

	private void bindUpdateValues(
			JdbcValueBindings jdbcValueBindings,
			PreparedStatement statement,
			List<ColumnValueParameter> updateParameters,
			String sql,
			SharedSessionContractImplementor session) {
		final var bindingGroup =
				jdbcValueBindings.getBindingGroup( tableMapping.getTableName() );
		if ( bindingGroup == null ) {
			throw new IllegalStateException(
					String.format(
							Locale.ROOT,
							"No value bindings for table on update : %s",
							tableMapping.getTableName()
					)
			);
		}

		int jdbcBindingPosition = 1;
		for ( var parameter : updateParameters ) {
			final var binding = findBinding( bindingGroup, parameter );
			if ( binding == null ) {
				throw new IllegalStateException(
						String.format(
								Locale.ROOT,
								"Missing value binding for update : %s.%s (%s)",
								tableMapping.getTableName(),
								parameter.getColumnReference().getColumnExpression(),
								parameter.getUsage()
						)
				);
			}
			try {
				binding.getValueBinder()
						.bind( statement, binding.getValue(), jdbcBindingPosition++, session );
			}
			catch (SQLException e) {
				throw session.getJdbcServices().getSqlExceptionHelper().convert(
						e,
						"Unable to bind parameter for upsert update",
						sql
				);
			}
		}
	}

	private static Binding findBinding(BindingGroup bindingGroup, ColumnValueParameter parameter) {
		final String columnName = parameter.getColumnReference().getColumnExpression();
		for ( var binding : bindingGroup.getBindings() ) {
			if ( binding.getValueDescriptor()
					.matches( columnName, parameter.getUsage() ) ) {
				return binding;
			}
		}
		return null;
	}
}
