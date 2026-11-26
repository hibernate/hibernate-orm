package org.hibernate.action.queue.internal.decompose.entity;


import jakarta.annotation.Nullable;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.spi.bind.Checkers;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.meta.EntityTableDescriptor;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.PartitionKeyValue;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/// Specialized BindPlan for soft delete operations.
///
/// Unlike hard deletes, soft deletes are actually `UPDATE` operations that set a
/// soft-delete indicator column.
///
/// @author Steve Ebersole
public class EntitySoftDeleteBindPlan implements BindPlan, OperationResultChecker {
	private final EntityTableDescriptor tableDescriptor;
	private final EntityPersister entityPersister;
	private final Object identifier;
	private final boolean applyVersion;
	private final Object version;
	private final Map<String, Object> optimisticLockBindings;
	private final List<PartitionKeyValue> partitionKeys;

	public EntitySoftDeleteBindPlan(
			EntityTableDescriptor tableDescriptor,
			EntityPersister entityPersister,
			Object identifier,
			boolean applyVersion,
			Object version,
			Map<String, Object> optimisticLockBindings,
			List<PartitionKeyValue> partitionKeys) {
		this.tableDescriptor = tableDescriptor;
		this.entityPersister = entityPersister;
		this.identifier = identifier;
		this.applyVersion = applyVersion;
		this.version = version;
		this.optimisticLockBindings = optimisticLockBindings;
		this.partitionKeys = partitionKeys;
	}

	@Override
	public @Nullable Object getEntityId() {
		return identifier;
	}

	@Override
	public void bindValues(
			JdbcValueBindings valueBindings,
			FlushOperation flushOperation,
			SharedSessionContractImplementor session) {
		TenantIdHelper.validateIdentifierTenant( identifier, entityPersister, session );
		TenantIdHelper.bindTenantRestriction( entityPersister, flushOperation.getJdbcOperation(), valueBindings, session );

		// NOTE: We do NOT bind the soft delete value or non-deleted restriction here.
		// These are literal values (e.g., true/false or CURRENT_TIMESTAMP) that are
		// already embedded in the SQL statement. They have no parameters to bind.

		// Bind the identifier for the WHERE clause
		breakDownKeyJdbcValue( valueBindings, session );

		// Apply optimistic locking if needed
		if ( applyVersion ) {
			applyVersionBasedOptLocking( valueBindings, session );
		}
		else {
			optimisticLockBindings.forEach( (column, value) ->
					valueBindings.bindValue( value, column, ParameterUsage.RESTRICT ) );
		}

		PartitionKeyValue.bindRestrictions(
				partitionKeys, tableDescriptor.name(), valueBindings, flushOperation.getJdbcOperation()
		);
	}

	private void breakDownKeyJdbcValue(
			JdbcValueBindings valueBindings,
			SharedSessionContractImplementor session) {
		entityPersister.getIdentifierMapping().breakDownJdbcValues(
				identifier,
				(index, jdbcValue, columnMapping) -> {
					valueBindings.bindValue(
							jdbcValue,
							tableDescriptor.keyDescriptor().columns().get( index ).name(),
							ParameterUsage.RESTRICT
					);
				},
				session
		);
	}

	protected void applyVersionBasedOptLocking(
			JdbcValueBindings jdbcValueBindings,
			SharedSessionContractImplementor session) {
		final var versionMapping = entityPersister.getVersionMapping();
		if ( versionMapping == null ) {
			return;
		}
		if ( tableDescriptor.name().equals( entityPersister.physicalTableNameForMutation( versionMapping ) ) ) {
			versionMapping.decompose(
					version,
					0,
					jdbcValueBindings,
					null,
					(valueIndex, bindings, noop, jdbcValue, selectableMapping) -> {
						bindings.bindValue(
								jdbcValue,
								selectableMapping.getSelectionExpression(),
								ParameterUsage.RESTRICT
						);
					},
					session
			);
		}
	}

	@Override
	public OperationResultChecker getOperationResultChecker() {
		return this;
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			PreparedStatement statement,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return Checkers.identifiedResultsCheck(
				tableDescriptor.deleteDetails().getExpectation(),
				affectedRowCount,
				statement,
				batchPosition,
				entityPersister,
				tableDescriptor,
				identifier,
				sqlString,
				sessionFactory
		);
	}
}
