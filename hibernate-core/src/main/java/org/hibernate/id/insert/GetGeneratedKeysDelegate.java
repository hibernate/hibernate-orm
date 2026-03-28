/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.action.queue.plan.PlannedOperation;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.spi.StatementPreparer;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.internal.util.MutableObject;
import org.hibernate.jdbc.Expectation;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static java.sql.Statement.RETURN_GENERATED_KEYS;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getActualGeneratedModelPart;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;
import static org.hibernate.internal.util.StringHelper.EMPTY_STRINGS;
import static org.hibernate.internal.util.StringHelper.unquote;

/**
 * Delegate for dealing with generated values using the JDBC3 method
 * {@link PreparedStatement#getGeneratedKeys()}.
 * <p>
 * Supports both {@link EventType#INSERT insert} and {@link EventType#UPDATE update} statements.
 *
 * @author Andrea Boriero
 */
public class GetGeneratedKeysDelegate extends AbstractReturningDelegate {
	private final String[] columnNames;

	public GetGeneratedKeysDelegate(
			EntityPersister persister,
			boolean inferredKeys,
			EventType timing) {
		super( persister, timing, !inferredKeys, false );

		if ( inferredKeys ) {
			columnNames = null;
		}
		else {
			final var resultBuilders = jdbcValuesMappingProducer.getResultBuilders();
			final List<String> columnNamesList = new ArrayList<>( resultBuilders.size() );
			final boolean unquote = dialect().unquoteGetGeneratedKeys();
			for ( var resultBuilder : resultBuilders ) {
				final String columnName =
						getActualGeneratedModelPart( resultBuilder.getModelPart() )
								.getSelectionExpression();
				columnNamesList.add( unquote ? unquote( columnName, dialect() ) : columnName );
			}
			columnNames = columnNamesList.toArray( EMPTY_STRINGS );
		}
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor factory) {
		final var identifierTableMapping = persister.getIdentifierTableMapping();
		return getTiming() == EventType.INSERT
				? new TableInsertBuilderStandard( persister, identifierTableMapping, factory )
				: new TableUpdateBuilderStandard<>( persister, identifierTableMapping, factory );
	}

	@Override
	public PreparedStatement prepareStatement(String sql, SharedSessionContractImplementor session) {
		var preparer = session.getJdbcCoordinator().getMutationStatementPreparer();
		return columnNames == null
				? preparer.prepareStatement( sql, RETURN_GENERATED_KEYS )
				: preparer.prepareStatement( sql, columnNames );
	}

	@Override
	public GeneratedValues performMutation(
			PreparedStatementDetails statementDetails,
			JdbcValueBindings jdbcValueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		final String sql = statementDetails.getSqlString();
		session.getJdbcServices().getSqlStatementLogger().logStatement( sql );
		try {
			final var preparedStatement = statementDetails.resolveStatement();
			jdbcValueBindings.beforeStatement( statementDetails );
			session.getJdbcCoordinator().getResultSetReturn().executeUpdate( preparedStatement, sql );
			return extractGeneratedValues( session, preparedStatement, sql,
					() -> String.format( Locale.ROOT,
							"Unable to extract generated key for '%s'",
							persister.getNavigableRole().getFullPath() ) );
		}
		finally {
			if ( statementDetails.getStatement() != null ) {
				statementDetails.releaseStatement( session );
			}
			jdbcValueBindings.afterStatement( statementDetails.getMutatingTableDetails() );
		}
	}

	@Override
	public GeneratedValues performGraphMutation(
			PlannedOperation operation,
			Object entity,
			SharedSessionContractImplementor session) {
		var jdbcOperation = (PreparableMutationOperation) operation.getJdbcOperation();
		var sql = jdbcOperation.getSqlString();
		session.getJdbcServices().getSqlStatementLogger().logStatement( sql );

		StatementPreparer preparer = session.getJdbcCoordinator().getStatementPreparer();
		try (var preparedStatement = columnNames == null
				? preparer.prepareStatement( sql, RETURN_GENERATED_KEYS )
				: preparer.prepareStatement( sql, columnNames ) ) {

			var valueBindings = new org.hibernate.action.queue.bind.JdbcValueBindings(
					operation.getMutatingTableDescriptor(),
					jdbcOperation
			);

			var ref = new MutableObject<GeneratedValues>();
			operation.getBindPlan().execute(
					(plannedOperation, binder, resultChecker) -> {
						binder.accept( valueBindings, session );
						valueBindings.beforeStatement( preparedStatement, session );

						session.getJdbcCoordinator().getResultSetReturn().executeUpdate( preparedStatement, sql );
						var generatedValues = extractGeneratedValues( session, preparedStatement, sql,
								() -> String.format( Locale.ROOT,
										"Unable to extract generated key for '%s'",
										persister.getNavigableRole().getFullPath() ) );
						ref.set( generatedValues );
					},
					operation,
					session
			);

			return ref.get();
		}
		catch (SQLException sqle) {
			throw session.getJdbcServices()
					.getSqlExceptionHelper()
					.convert( sqle, "Unable performing SQL - " + sql );
		}
		finally {
			session.getJdbcCoordinator().afterStatementExecution();
		}
	}

	@Override
	public GeneratedValues executeAndExtractReturning(
			String sql,
			PreparedStatement preparedStatement,
			SharedSessionContractImplementor session) {
		session.getJdbcCoordinator().getResultSetReturn().executeUpdate( preparedStatement, sql );
		return extractGeneratedValues( session, preparedStatement, sql,
				() -> "Unable to extract generated keys from ResultSet" );
	}

	private @Nullable GeneratedValues extractGeneratedValues(
			SharedSessionContractImplementor session,
			PreparedStatement preparedStatement,
			String sql,
			Supplier<String> message) {
		final var jdbcServices = session.getJdbcServices();
		final var jdbcCoordinator = session.getJdbcCoordinator();
		try {
			final var resultSet = preparedStatement.getGeneratedKeys();
			try {
				return getGeneratedValues( resultSet, preparedStatement, persister, getTiming(), session );
			}
			catch (SQLException e) {
				throw jdbcServices.getSqlExceptionHelper().convert( e, message.get(), sql );
			}
			finally {
				if ( resultSet != null ) {
					jdbcCoordinator.getLogicalConnection().getResourceRegistry()
							.release( resultSet, preparedStatement );
				}
			}
		}
		catch (SQLException e) {
			throw jdbcServices.getSqlExceptionHelper().convert(
					e,
					"Unable to extract generated keys from ResultSet",
					sql
			);
		}
	}
}
