/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import jakarta.persistence.QueryFlushMode;

import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.internal.TenantIdGeneration;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.type.ComponentType;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.EventType.INSERT;

/**
 * Tenant ownership checks for mutations which start with an unloaded or detached entity.
 */
public final class TenantIdHelper {
	private TenantIdHelper() {
	}

	public static AttributeMapping tenantIdMapping(EntityPersister persister) {
		final var generators = persister.getGenerators();
		for ( int i = 0; i < generators.length; i++ ) {
			if ( generators[i] instanceof TenantIdGeneration ) {
				return persister.getAttributeMapping( i );
			}
		}
		return null;
	}

	public static void applyTenantRestriction(EntityPersister persister, RestrictedTableMutationBuilder<?, ?> builder) {
		final var tenantMapping = tenantIdMapping( persister );
		if ( tenantMapping != null && builder.getOptimisticLockBindings() != null ) {
			final var selectable = tenantMapping.getSelectable( 0 );
			if ( selectable.getContainingTableExpression().equals( builder.getMutatingTable().getTableName() ) ) {
				builder.getOptimisticLockBindings().addTenantRestriction( selectable );
			}
		}
	}

	public static void bindTenantRestriction(
			EntityPersister persister, String tableName, JdbcValueBindings bindings, SharedSessionContractImplementor session) {
		final var tenantMapping = tenantIdMapping( persister );
		if ( tenantMapping != null ) {
			final var selectable = tenantMapping.getSelectable( 0 );
			if ( selectable.getContainingTableExpression().equals( tableName ) ) {
				bindings.bindValue( isRoot( session ) ? null : session.getTenantIdentifierValue(),
						selectable.getSelectionExpression(), ParameterUsage.TENANT );
			}
		}
	}

	public static boolean isRoot(SharedSessionContractImplementor session) {
		final var resolver = session.getFactory().getCurrentTenantIdentifierResolver();
		return resolver != null && resolver.isRoot( session.getTenantIdentifierValue() );
	}

	public static void checkIdentifierTenant(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		if ( !isRoot( session ) ) {
			final var generator = persister.getGenerator();
			if ( generator instanceof TenantIdGeneration ) {
				checkIdentifierTenantValue( id, id, persister, session );
			}
			else if ( id != null && generator instanceof CompositeNestedGeneratedValueGenerator composite ) {
				final var type = (ComponentType) persister.getIdentifierType();
				for ( var plan : composite.getGenerationPlans() ) {
					if ( plan.getGenerator() instanceof TenantIdGeneration ) {
						checkIdentifierTenantValue( id,
								type.getPropertyValue( id, plan.getPropertyIndex(), session ), persister, session );
					}
				}
			}
		}
	}

	private static void checkIdentifierTenantValue(
			Object id, Object tenant, EntityPersister persister, SharedSessionContractImplementor session) {
		if ( !session.getFactory().getTenantIdentifierJavaType().areEqual( tenant, session.getTenantIdentifierValue() ) ) {
			throw new StaleObjectStateException( persister.getEntityName(), id );
		}
	}

	public static void initializeIdentifierTenant(
			Object entity, EntityPersister persister, SharedSessionContractImplementor session) {
		final var generator = persister.getGenerator();
		if ( generator instanceof TenantIdGeneration tenantGenerator ) {
			persister.setIdentifier( entity, tenantGenerator.generate(
					session, entity, persister.getIdentifier( entity, session ), INSERT ), session );
		}
		else if ( generator instanceof CompositeNestedGeneratedValueGenerator composite ) {
			final Object id = persister.getIdentifier( entity, session );
			if ( id != null ) {
				final var type = (ComponentType) persister.getIdentifierType();
				final Object[] values = type.getPropertyValues( id, session );
				boolean generated = false;
				for ( var plan : composite.getGenerationPlans() ) {
					if ( plan.getGenerator() instanceof TenantIdGeneration tenantGenerator ) {
						final int position = plan.getPropertyIndex();
						values[position] = tenantGenerator.generate( session, entity, values[position], INSERT );
						generated = true;
					}
				}
				if ( generated ) {
					persister.setIdentifier( entity, type.replacePropertyValues( id, values, session ), session );
				}
			}
		}
	}

	public static void initializeTenantId(
			Object entity, Object[] state, EntityPersister persister, SharedSessionContractImplementor session) {
		final var tenantMapping = tenantIdMapping( persister );
		if ( tenantMapping != null ) {
			final int position = tenantMapping.getStateArrayPosition();
			final var generator = (TenantIdGeneration) persister.getGenerators()[position];
			state[position] = generator.generate( session, entity, state[position], INSERT );
			persister.setValue( entity, position, state[position] );
		}
	}

	/**
	 * Check the stored owner before scheduling collection or secondary-table mutations.
	 * The supplied entity state cannot establish ownership of a database row.
	 */
	public static void checkTenantId(
			Object id, EntityPersister persister, SharedSessionContractImplementor session, boolean allowMissing) {
		checkIdentifierTenant( id, persister, session );
		final var tenantMapping = tenantIdMapping( persister );
		if ( tenantMapping != null && !isRoot( session ) ) {
			if ( session.isTransactionInProgress()
					&& ( persister.hasMultipleTables() || persister.hasOwnedCollections() ) ) {
				// Keep the owner stable while its other tables are being mutated.
				final String tenantPath = "e." + tenantMapping.getAttributeName();
				final Object owner = session.createSelectionQuery(
						"select " + tenantPath + " from " + persister.getEntityName()
								+ " e where id(e) = :id and " + tenantPath + " = :tenant", Object.class )
						.setParameter( "id", id )
						.setParameter( "tenant", session.getTenantIdentifierValue() )
						.setQueryFlushMode( QueryFlushMode.NO_FLUSH )
						.setHibernateLockMode( LockMode.PESSIMISTIC_WRITE )
						.getSingleResultOrNull();
				if ( owner != null || allowMissing && persister.getDatabaseSnapshot( id, session ) == null ) {
					return;
				}
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
			final Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			if ( snapshot == null ? !allowMissing
					: !session.getFactory().getTenantIdentifierJavaType().areEqual(
							snapshot[tenantMapping.getStateArrayPosition()], session.getTenantIdentifierValue() ) ) {
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
		}
	}
}
