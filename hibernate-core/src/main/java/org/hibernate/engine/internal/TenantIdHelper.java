/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import jakarta.persistence.QueryFlushMode;

import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.LockOptions;
import org.hibernate.loader.ast.internal.LoaderSelectBuilder;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.generator.internal.TenantIdGeneration;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.type.ComponentType;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.generator.EventType.INSERT;
import static java.util.Collections.singletonList;

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
			if ( generator instanceof TenantIdGeneration tenantGenerator ) {
				tenantGenerator.validateTenantId( session, id );
			}
			else if ( id != null && generator instanceof CompositeNestedGeneratedValueGenerator composite ) {
				final var type = (ComponentType) persister.getIdentifierType();
				for ( var plan : composite.getGenerationPlans() ) {
					if ( plan.getGenerator() instanceof TenantIdGeneration tenantGenerator ) {
						tenantGenerator.validateTenantId( session,
								type.getPropertyValue( id, plan.getPropertyIndex(), session ) );
					}
				}
			}
		}
	}

	/**
	 * Validate the detached tenant value before any SQL or changes to the entity state.
	 */
	public static void validateTenantId(
			Object entity, Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		checkIdentifierTenant( id, persister, session );
		final var tenantMapping = tenantIdMapping( persister );
		if ( tenantMapping != null ) {
			final int position = tenantMapping.getStateArrayPosition();
			final var generator = (TenantIdGeneration) persister.getGenerators()[position];
			generator.validateTenantId( session, persister.getValue( entity, position ) );
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
				if ( owner != null || allowMissing && !rowExists( id, persister, session ) ) {
					return;
				}
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
			final Object[] snapshot = persister.getDatabaseSnapshot( id, session );
			if ( snapshot == null ? !allowMissing || rowExists( id, persister, session )
					: !session.getFactory().getTenantIdentifierJavaType().areEqual(
							snapshot[tenantMapping.getStateArrayPosition()], session.getTenantIdentifierValue() ) ) {
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
		}
	}

	private static boolean rowExists(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		// A tenant-filtered snapshot cannot distinguish an absent row from another tenant's row.
		// Before treating a row as absent, check existence without reading entity state.
		final var factory = session.getFactory();
		final var identifier = persister.getIdentifierMapping();
		final var parameters = JdbcParametersList.newBuilder();
		final var select = LoaderSelectBuilder.createSelect(
				persister, singletonList( identifier ), identifier, null, 1,
				new LoadQueryInfluencers( factory ), LockOptions.NONE, parameters::add,
				new SqlAliasBaseManager(), factory
		);
		final var jdbcParameters = parameters.build();
		final var bindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		bindings.registerParametersForEachJdbcValue( id, identifier, jdbcParameters, session );
		final var jdbcSelect = factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.Select( factory, select ) )
				.translate( bindings, QueryOptions.NONE );
		return !session.getJdbcServices().getJdbcSelectExecutor().list(
				jdbcSelect, bindings, new BaseExecutionContext( session ),
				RowTransformerSingularReturnImpl.instance(), null, ListResultsConsumer.UniqueSemantic.FILTER, 1
		).isEmpty();
	}
}
