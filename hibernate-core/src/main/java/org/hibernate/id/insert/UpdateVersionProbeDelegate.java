/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.action.queue.spi.bind.DelayedValueAccess;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.JdbcValueBindingsImplementor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.AbstractGeneratedValuesMutationDelegate;
import org.hibernate.generator.values.GeneratedValues;
import org.hibernate.generator.values.internal.GeneratedValuesImpl;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityTableMapping;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.hibernate.sql.model.ast.builder.TableMutationBuilder;
import org.hibernate.sql.model.ast.builder.TableUpdateBuilderStandard;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;

/**
 * Resolves an application-generated version after a conditional update on dialects which do not
 * support update returning.  The primary update is followed by a no-op update restricted by the
 * candidate version.  Its row count tells us which arm of the conditional version assignment won,
 * without selecting an entity snapshot.
 */
public class UpdateVersionProbeDelegate extends AbstractGeneratedValuesMutationDelegate {
	private final EntityTableMapping tableMapping;
	private final EntityVersionMapping versionMapping;
	private final String probeSql;

	public UpdateVersionProbeDelegate(EntityPersister persister, List<? extends ModelPart> generatedProperties) {
		super( persister, EventType.UPDATE, true, false, generatedProperties );
		tableMapping = persister.getIdentifierTableMapping();
		versionMapping = persister.getVersionMapping();
		probeSql = buildProbeSql();
	}

	@Override
	public TableMutationBuilder<?> createTableMutationBuilder(
			Expectation expectation,
			SessionFactoryImplementor sessionFactory) {
		return new TableUpdateBuilderStandard<>( persister, tableMapping, sessionFactory );
	}

	@Override
	public PreparedStatement prepareStatement(String sql, SharedSessionContractImplementor session) {
		return session.getJdbcCoordinator().getMutationStatementPreparer()
				.prepareStatement( sql, NO_GENERATED_KEYS );
	}

	@Override
	public GeneratedValues performMutation(
			PreparedStatementDetails statementDetails,
			JdbcValueBindings valueBindings,
			Object entity,
			SharedSessionContractImplementor session) {
		final String sql = statementDetails.getSqlString();
		logSql( sql, session );
		final Object oldVersion = boundVersion( valueBindings, ParameterUsage.RESTRICT );
		final Object newVersion = boundVersion( valueBindings, ParameterUsage.SET );
		final Object id = persister.getIdentifier( entity, session );
		try {
			valueBindings.beforeStatement( statementDetails );
			final int rowCount = session.getJdbcCoordinator().getResultSetReturn()
					.executeUpdate( statementDetails.resolveStatement(), sql );
			identifiedResultsCheck( statementDetails, rowCount, -1, persister, id, session.getFactory() );
		}
		finally {
			if ( statementDetails.getStatement() != null ) {
				statementDetails.releaseStatement( session );
			}
			valueBindings.afterStatement( statementDetails.getMutatingTableDetails() );
			session.getJdbcCoordinator().afterStatementExecution();
		}
		return generatedVersion( resolveVersion( id, oldVersion, newVersion, session ) );
	}

	@Override
	public GeneratedValues performGraphMutation(
			FlushOperation operation,
			Object entity,
			SharedSessionContractImplementor session) {
		final var jdbcOperation = (PreparableMutationOperation) operation.getJdbcOperation();
		final String sql = jdbcOperation.getSqlString();
		logSql( sql, session );
		final PreparedStatement statement = prepareStatement( sql, session );
		final Object oldVersion;
		final Object newVersion;
		try {
			final var valueBindings = new org.hibernate.action.queue.spi.bind.JdbcValueBindings(
					operation.getMutatingTableDescriptor(),
					jdbcOperation
			);
			operation.getBindPlan().bindValues( valueBindings, operation, session );
			oldVersion = resolvedValue( valueBindings.getBoundValue(
					versionMapping.getSelectionExpression(),
					ParameterUsage.RESTRICT
			) );
			newVersion = resolvedValue( valueBindings.getBoundValue(
					versionMapping.getSelectionExpression(),
					ParameterUsage.SET
			) );
			valueBindings.beforeStatement( statement, session );
			final int rowCount = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( statement, sql );
			operation.checkResult( rowCount, -1, sql, session.getFactory() );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "Unable to execute update", sql );
		}
		finally {
			session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( statement );
			session.getJdbcCoordinator().afterStatementExecution();
		}
		return generatedVersion( resolveVersion(
				operation.getBindPlan().getEntityId(),
				oldVersion,
				newVersion,
				session
		) );
	}

	private Object resolveVersion(
			Object id,
			Object oldVersion,
			Object newVersion,
			SharedSessionContractImplementor session) {
		if ( persister.getVersionType().isEqual( oldVersion, newVersion ) ) {
			return oldVersion;
		}
		return probeVersion( id, newVersion, session ) ? newVersion : oldVersion;
	}

	private boolean probeVersion(Object id, Object version, SharedSessionContractImplementor session) {
		logSql( probeSql, session );
		final PreparedStatement statement = prepareStatement( probeSql, session );
		try {
			final int[] position = { 1 };
			tableMapping.getKeyMapping().breakDownKeyJdbcValues(
					id,
					(jdbcValue, column) -> {
						try {
							column.getJdbcMapping().getJdbcValueBinder()
									.bind( statement, jdbcValue, position[0]++, session );
						}
						catch (SQLException e) {
							throw session.getJdbcServices().getSqlExceptionHelper()
									.convert( e, "Unable to bind version probe", probeSql );
						}
					},
					session
			);
			versionMapping.getJdbcMapping().getJdbcValueBinder()
					.bind( statement, version, position[0], session );
			return session.getJdbcCoordinator().getResultSetReturn().executeUpdate( statement, probeSql ) == 1;
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "Unable to execute version probe", probeSql );
		}
		finally {
			session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( statement );
			session.getJdbcCoordinator().afterStatementExecution();
		}
	}

	private Object boundVersion(JdbcValueBindings valueBindings, ParameterUsage usage) {
		final var implementor = (JdbcValueBindingsImplementor) valueBindings;
		return resolvedValue( implementor.getBoundValue(
				tableMapping.getTableName(),
				versionMapping.getSelectionExpression(),
				usage
		) );
	}

	private static Object resolvedValue(Object value) {
		return value instanceof DelayedValueAccess delayedValue ? delayedValue.get() : value;
	}

	private GeneratedValues generatedVersion(Object version) {
		final var generatedValues = new GeneratedValuesImpl( 1 );
		generatedValues.addGeneratedValue( versionMapping, version );
		return generatedValues;
	}

	private String buildProbeSql() {
		final String versionColumn = versionMapping.getSelectionExpression();
		final var sql = new StringBuilder( "update " )
				.append( tableMapping.getTableName() )
				.append( " set " )
				.append( versionColumn )
				.append( '=' )
				.append( versionColumn )
				.append( " where " );
		tableMapping.getKeyMapping().forEachKeyColumn( (index, column) -> {
			if ( index > 0 ) {
				sql.append( " and " );
			}
			sql.append( column.getColumnName() ).append( "=?" );
		} );
		return sql.append( " and " ).append( versionColumn ).append( "=?" ).toString();
	}

	private static void logSql(String sql, SharedSessionContractImplementor session) {
		session.getJdbcServices().getSqlStatementLogger().logStatement( sql );
		session.getJdbcSessionContext().getStatementObserver().performingSql( sql, -1 );
	}
}
