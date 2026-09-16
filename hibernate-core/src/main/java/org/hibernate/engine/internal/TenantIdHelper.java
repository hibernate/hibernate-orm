/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import org.hibernate.jdbc.Expectation;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;

/**
 * Tenant ownership checks for entity mutations.
 */
public final class TenantIdHelper {
	private TenantIdHelper() {
	}

	public static AttributeMapping tenantIdMapping(EntityPersister persister) {
		final var mapping = persister.getTenantIdMapping();
		return mapping == null ? null : mapping.getAttributeMapping();
	}

	public static void applyTenantRestriction(EntityPersister persister, RestrictedTableMutationBuilder<?, ?> builder) {
		final var tenantMapping = tenantIdMapping( persister );
		if ( tenantMapping != null && builder.getOptimisticLockBindings() != null ) {
			final var selectable = tenantMapping.getSelectable( 0 );
			if ( persister.physicalTableNameForMutation( selectable ).equals( builder.getMutatingTable().getTableName() ) ) {
				builder.getOptimisticLockBindings().addTenantRestriction( selectable );
			}
		}
	}

	public static void bindTenantRestriction(
			EntityPersister persister, MutationOperation operation, JdbcValueBindings bindings,
			SharedSessionContractImplementor session) {
		final var tenantMapping = tenantIdMapping( persister );
		if ( tenantMapping != null ) {
			final var selectable = tenantMapping.getSelectable( 0 );
			if ( persister.physicalTableNameForMutation( selectable ).equals( operation.getTableDetails().getTableName() )
					&& operation.findValueDescriptor( selectable.getSelectionExpression(), ParameterUsage.TENANT ) != null ) {
				bindings.bindValue( session.isRootTenant() ? null : session.getTenantIdentifierValue(),
						selectable.getSelectionExpression(), ParameterUsage.TENANT );
			}
		}
	}

	public static boolean needsMultiTableUpdateCheck(
			EntityPersister persister, SharedSessionContractImplementor session) {
		return persister.hasMultipleTables() && !session.isRootTenant() && tenantIdMapping( persister ) != null;
	}

	/**
	 * Whether successful execution of this update establishes and locks tenant ownership.
	 * Custom SQL and optional row counts cannot provide this guarantee.
	 */
	public static boolean checksTenantId(EntityPersister persister, MutationOperation operation) {
		if ( operation instanceof PreparableMutationOperation preparable
				&& operation.getMutationType() == MutationType.UPDATE
				&& !operation.getTableDetails().isOptional()
				&& operation.getTableDetails().getUpdateDetails().getCustomSql() == null
				&& preparable.getExpectation() instanceof Expectation.RowCount ) {
			final var tenantMapping = tenantIdMapping( persister );
			if ( tenantMapping != null ) {
				final var selectable = tenantMapping.getSelectable( 0 );
				return persister.physicalTableNameForMutation( selectable ).equals( operation.getTableDetails().getTableName() )
					&& operation.findValueDescriptor( selectable.getSelectionExpression(), ParameterUsage.TENANT ) != null;
			}
		}
		return false;
	}

	/**
	 * Whether a composite identifier contains an unassigned generated tenant id.
	 * Such an incomplete primary key cannot identify a stored row.
	 */
	public static boolean hasUnassignedIdentifierTenant(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		final var mapping = persister.getTenantIdMapping();
		return mapping != null && mapping.hasUnassignedIdentifierTenant( id, session );
	}

	public static void validateIdentifierTenant(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		final var mapping = persister.getTenantIdMapping();
		if ( mapping != null ) {
			mapping.validateIdentifier( id, session );
		}
	}

	/**
	 * Validate the detached tenant value before any SQL or changes to the entity state.
	 */
	public static void validateAssignedTenantId(
			Object entity, Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		final var mapping = persister.getTenantIdMapping();
		if ( mapping != null ) {
			mapping.validateAssignedValue( entity, id, session );
		}
	}

	public static void initializeIdentifierTenant(
			Object entity, EntityPersister persister, SharedSessionContractImplementor session) {
		final var mapping = persister.getTenantIdMapping();
		if ( mapping != null ) {
			mapping.initializeIdentifier( entity, session );
		}
	}

	public static void initializeTenantId(
			Object entity, Object[] state, EntityPersister persister, SharedSessionContractImplementor session) {
		final var mapping = persister.getTenantIdMapping();
		if ( mapping != null ) {
			mapping.initialize( entity, state, session );
		}
	}

	/**
	 * Check the stored owner before scheduling collection or secondary-table mutations.
	 * The supplied entity state cannot establish ownership of a database row.
	 */
	public static void checkStoredTenantOwnership(
			Object id, EntityPersister persister, SharedSessionContractImplementor session, MissingRowPolicy missingRowPolicy) {
		validateIdentifierTenant( id, persister, session );
		final var loader = persister.getTenantIdLoader();
		if ( loader != null && !session.isRootTenant() ) {
			final boolean lock = session.isTransactionInProgress()
					&& ( persister.hasMultipleTables() || persister.hasOwnedCollections() );
			if ( !loader.belongsToTenant( id, lock, session )
					&& ( missingRowPolicy == MissingRowPolicy.THROW || loader.rowExists( id, session ) ) ) {
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
		}
	}

	public enum MissingRowPolicy {
		ALLOW, THROW
	}
}
