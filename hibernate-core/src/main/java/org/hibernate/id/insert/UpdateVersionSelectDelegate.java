package org.hibernate.id.insert;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hibernate.StaleObjectStateException;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
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
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.sql.ast.spi.model.builder.TableMutationBuilder;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;

import static java.sql.Statement.NO_GENERATED_KEYS;
import static org.hibernate.engine.jdbc.mutation.internal.ModelMutationHelper.identifiedResultsCheck;
import static org.hibernate.generator.values.internal.GeneratedValuesHelper.getGeneratedValues;

/**
 * Resolves an application-generated version after a conditional update on dialects which do not
 * support update returning. The primary update is followed by a select of the version column
 * and any other generated columns, using the dialect's current-read semantics without
 * selecting an entity snapshot.
 */
public class UpdateVersionSelectDelegate extends AbstractGeneratedValuesMutationDelegate {
	private final EntityTableMapping tableMapping;
	private final EntityVersionMapping versionMapping;
	private final String generatedValuesSelect;

	public UpdateVersionSelectDelegate(EntityPersister persister, List<? extends ModelPart> generatedProperties) {
		super( persister, EventType.UPDATE, true, false, generatedProperties );
		tableMapping = persister.getIdentifierTableMapping();
		versionMapping = persister.getVersionMapping();
		if ( generatedProperties.size() > 1 ) {
			final var select = new SimpleSelect( persister.getFactory() )
					.setTableName( tableMapping.getTableName() )
					.setCurrentRead( true );
			for ( var property : generatedProperties ) {
				select.addColumn( property.asBasicValuedModelPart().getSelectionExpression() );
			}
			tableMapping.getKeyMapping().forEachKeyColumn( (i, column) -> select.addRestriction( column.getColumnName() ) );
			generatedValuesSelect = select.toStatementString();
		}
		else {
			generatedValuesSelect = null;
		}
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
		return selectVersion( id, session );
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
		try {
			final var valueBindings = new org.hibernate.action.queue.spi.bind.JdbcValueBindings(
					operation.getMutatingTableDescriptor(),
					jdbcOperation
			);
			operation.getBindPlan().bindValues( valueBindings, operation, session );
			valueBindings.beforeStatement( statement, session );
			final int rowCount = session.getJdbcCoordinator().getResultSetReturn().executeUpdate( statement, sql );
			operation.checkResult( rowCount, statement, -1, sql, session.getFactory() );
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e, "Unable to execute update", sql );
		}
		finally {
			session.getJdbcCoordinator().getLogicalConnection().getResourceRegistry().release( statement );
			session.getJdbcCoordinator().afterStatementExecution();
		}
		return selectVersion( operation.getBindPlan().getEntityId(), session );
	}

	private GeneratedValues selectVersion(Object id, SharedSessionContractImplementor session) {
		if ( generatedValuesSelect != null ) {
			return selectGeneratedValues( id, session );
		}
		final Object version = persister.getCurrentVersion( id, session );
		if ( version == null ) {
			throw new StaleObjectStateException( persister.getEntityName(), id );
		}
		final var generatedValues = new GeneratedValuesImpl( 1 );
		generatedValues.addGeneratedValue( versionMapping, version );
		return generatedValues;
	}

	private GeneratedValues selectGeneratedValues(Object id, SharedSessionContractImplementor session) {
		final var jdbcCoordinator = session.getJdbcCoordinator();
		final var statement = jdbcCoordinator.getStatementPreparer().prepareStatement( generatedValuesSelect );
		final var resourceRegistry = jdbcCoordinator.getLogicalConnection().getResourceRegistry();
		try {
			persister.getIdentifierType().nullSafeSet( statement, id, 1, session );
			final var resultSet = jdbcCoordinator.getResultSetReturn().extract( statement, generatedValuesSelect );
			try {
				return getGeneratedValues( resultSet, statement, persister, EventType.UPDATE, session );
			}
			finally {
				resourceRegistry.release( resultSet, statement );
			}
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert(
					e, "Could not retrieve generated values: " + persister.getEntityName(), generatedValuesSelect );
		}
		finally {
			resourceRegistry.release( statement );
			jdbcCoordinator.afterStatementExecution();
		}
	}

	private static void logSql(String sql, SharedSessionContractImplementor session) {
		session.getJdbcServices().getSqlStatementLogger().logStatement( sql );
		session.getJdbcSessionContext().getStatementObserver().performingSql( sql, -1 );
	}
}
