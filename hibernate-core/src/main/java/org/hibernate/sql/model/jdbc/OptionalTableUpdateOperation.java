/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.internal.JdbcValueDescriptorImpl;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.engine.jdbc.mutation.internal.PreparedStatementGroupSingleTable;
import org.hibernate.engine.jdbc.mutation.spi.Binding;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.MutationStatementPreparer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.persister.entity.mutation.UpdateValuesAnalysis;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;
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

		this.jdbcValueDescriptors = CollectionHelper.arrayList( parameters.size() );
		for ( int i = 0; i < parameters.size(); i++ ) {
			final ColumnValueParameter valueParameter = parameters.get( i );
			jdbcValueDescriptors.add( new JdbcValueDescriptorImpl( valueParameter, i+1 ) );
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

	@Override
	public JdbcValueDescriptor findValueDescriptor(String columnName, ParameterUsage usage) {
		for ( int i = 0; i < jdbcValueDescriptors.size(); i++ ) {
			final JdbcValueDescriptor descriptor = jdbcValueDescriptors.get( i );
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
		final UpdateValuesAnalysis valuesAnalysis = (UpdateValuesAnalysis) incomingValuesAnalysis;
		if ( !valuesAnalysis.getTablesNeedingUpdate().contains( tableMapping )
				&& !valuesAnalysis.getTablesNeedingDynamicUpdate().contains( tableMapping ) ) {
			return;
		}

		try {
			if ( !valuesAnalysis.getTablesWithNonNullValues().contains( tableMapping ) ) {
				// all the new values for this table were null - possibly delete the row
				if ( valuesAnalysis.getTablesWithPreviousNonNullValues().contains( tableMapping ) ) {
					performDelete( jdbcValueBindings, session );
				}
			}
			else {
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
					MODEL_MUTATION_LOGGER.debugf(
							"Upsert update altered no rows - inserting : %s",
							tableMapping.getTableName()
					);
					performInsert( jdbcValueBindings, session );
				}
			}
		}
		finally {
			jdbcValueBindings.afterStatement( tableMapping );
		}

	}

	private void performDelete(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final JdbcDeleteMutation jdbcDelete = createJdbcDelete( session );

		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final PreparedStatement deleteStatement = createStatementDetails( jdbcDelete, jdbcCoordinator );
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
		final BindingGroup bindingGroup = jdbcValueBindings.getBindingGroup( tableMapping.getTableName() );
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

		final Set<Binding> bindings = bindingGroup.getBindings();
		// leverage the fact that bindings are contiguous to avoid full nested iterations
		final Iterator<ColumnValueBinding> keyBindingsItr = keyBindings.iterator();

		bindings: for ( Binding binding : bindings ) {
			// binding-position here is 1-based (JDBC)
			final JdbcValueDescriptor valueDescriptor = jdbcValueDescriptors.get( binding.getPosition() - 1 );

			// key bindings would have a usage of RESTRICT relative to the UPDATE
			if ( valueDescriptor.getUsage() != ParameterUsage.RESTRICT ) {
				continue;
			}

			while ( keyBindingsItr.hasNext() ) {
				final ColumnValueBinding valueBinding = keyBindingsItr.next();

				if ( Objects.equals( valueBinding.getColumnReference().getColumnExpression(), binding.getColumnName() ) ) {
					// `binding` is for a key column
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

	private JdbcDeleteMutation createJdbcDelete(SharedSessionContractImplementor session) {
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

		final SessionFactoryImplementor factory = session.getSessionFactory();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = factory
				.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory();

		final SqlAstTranslator<JdbcDeleteMutation> translator = sqlAstTranslatorFactory.buildModelMutationTranslator(
				tableDelete,
				factory
		);

		return translator.translate( null, MutationQueryOptions.INSTANCE );
	}

	private boolean performUpdate(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		MODEL_MUTATION_LOGGER.tracef( "#performUpdate(%s)", tableMapping.getTableName() );

		final TableUpdate<JdbcMutationOperation> tableUpdate;
		if ( tableMapping.getUpdateDetails() != null
				&& tableMapping.getUpdateDetails().getCustomSql() != null ) {
			tableUpdate = new TableUpdateCustomSql(
					new MutatingTableReference( tableMapping ),
					mutationTarget,
					"upsert update for " + mutationTarget.getRolePath(),
					valueBindings,
					keyBindings,
					optimisticLockBindings,
					parameters
			);
		}
		else {
			tableUpdate = new TableUpdateStandard(
					new MutatingTableReference( tableMapping ),
					mutationTarget,
					"upsert update for " + mutationTarget.getRolePath(),
					valueBindings,
					keyBindings,
					optimisticLockBindings,
					parameters
			);
		}

		final SqlAstTranslator<JdbcMutationOperation> translator = session
				.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory()
				.buildModelMutationTranslator( tableUpdate, session.getFactory() );

		final JdbcMutationOperation jdbcUpdate = translator.translate( null, MutationQueryOptions.INSTANCE );

		final PreparedStatementGroupSingleTable statementGroup = new PreparedStatementGroupSingleTable( jdbcUpdate, session );
		final PreparedStatementDetails statementDetails = statementGroup.resolvePreparedStatementDetails( tableMapping.getTableName() );

		try {
			final PreparedStatement updateStatement = statementDetails.resolveStatement();

			session.getJdbcServices().getSqlStatementLogger().logStatement( statementDetails.getSqlString() );

			jdbcValueBindings.beforeStatement( statementDetails );

			final int rowCount = session.getJdbcCoordinator().getResultSetReturn()
					.executeUpdate( updateStatement, statementDetails.getSqlString() );

			if ( rowCount == 0 ) {
				return false;
			}

			expectation.verifyOutcome(
					rowCount,
					updateStatement,
					-1,
					statementDetails.getSqlString()
			);

			return true;
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e,
					"Unable to execute mutation PreparedStatement against table `" + tableMapping.getTableName() + "`",
					statementDetails.getSqlString()
			);
		}
		finally {
			statementDetails.releaseStatement( session );
		}
	}

	private void performInsert(JdbcValueBindings jdbcValueBindings, SharedSessionContractImplementor session) {
		final JdbcInsertMutation jdbcInsert = createJdbcInsert( session );

		final JdbcCoordinator jdbcCoordinator = session.getJdbcCoordinator();
		final PreparedStatement insertStatement = createStatementDetails( jdbcInsert, jdbcCoordinator );

		try {
			session.getJdbcServices().getSqlStatementLogger().logStatement( jdbcInsert.getSqlString() );

			final BindingGroup bindingGroup = jdbcValueBindings.getBindingGroup( tableMapping.getTableName() );
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
							throw session.getJdbcServices().getSqlExceptionHelper().convert(
									e,
									"Unable to bind parameter for upsert insert",
									jdbcInsert.getSqlString()
							);
						}
					}
				} );
			}

			jdbcCoordinator.getResultSetReturn()
					.executeUpdate( insertStatement, jdbcInsert.getSqlString() );
		}
		finally {
			jdbcCoordinator.getLogicalConnection().getResourceRegistry().release( insertStatement );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	private JdbcInsertMutation createJdbcInsert(SharedSessionContractImplementor session) {
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

		final SessionFactoryImplementor factory = session.getSessionFactory();
		final SqlAstTranslatorFactory sqlAstTranslatorFactory = factory
				.getJdbcServices()
				.getJdbcEnvironment()
				.getSqlAstTranslatorFactory();

		final SqlAstTranslator<JdbcInsertMutation> translator = sqlAstTranslatorFactory.buildModelMutationTranslator(
				tableInsert,
				factory
		);

		return translator.translate( null, MutationQueryOptions.INSTANCE );
	}

	private static PreparedStatement createStatementDetails(
			PreparableMutationOperation operation,
			JdbcCoordinator jdbcCoordinator) {
		final MutationStatementPreparer statementPreparer = jdbcCoordinator.getMutationStatementPreparer();
		final PreparedStatement statement = statementPreparer.prepareStatement( operation.getSqlString(), false );
		jdbcCoordinator.getLogicalConnection().getResourceRegistry().register( null, statement );
		return statement;
	}

	@Override
	public String toString() {
		return "OptionalTableUpdateOperation(" + tableMapping + ")";
	}
}
