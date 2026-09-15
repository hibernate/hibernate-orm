/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.internal;

import java.sql.SQLException;

import org.hibernate.LockMode;
import org.hibernate.jdbc.Expectation;
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
import org.hibernate.generator.Generator;
import org.hibernate.generator.internal.CompositeGeneratorBuilder.CompositeGenerator;
import org.hibernate.id.CompositeNestedGeneratedValueGenerator;
import org.hibernate.type.ComponentType;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.sql.ast.spi.model.builder.RestrictedTableMutationBuilder;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.type.Type;

import static org.hibernate.generator.EventType.INSERT;
import static java.util.Collections.singletonList;

/**
 * Tenant ownership checks for entity mutations.
 */
public final class TenantIdHelper {
	private TenantIdHelper() {
	}

	public static AttributeMapping tenantIdMapping(EntityPersister persister) {
		final var mapping = tenantIdMapping( persister, persister.getGenerators() );
		return mapping == null ? null : mapping.leafAttribute();
	}

	private static TenantIdMapping tenantIdMapping(ManagedMappingType type, Generator[] generators) {
		for ( int i = 0; i < generators.length; i++ ) {
			final var attribute = type.getAttributeMapping( i );
			if ( generators[i] instanceof TenantIdGeneration generator ) {
				return new TenantIdMapping( attribute, generator, null );
			}
			else if ( generators[i] instanceof CompositeGenerator composite ) {
				final var nested = tenantIdMapping(
						attribute.asEmbeddedAttributeMapping().getEmbeddableTypeDescriptor(),
						composite.generators().toArray( Generator[]::new ) );
				if ( nested != null ) {
					return new TenantIdMapping( attribute, null, nested );
				}
			}
		}
		return null;
	}

	private record TenantIdMapping(AttributeMapping attribute, TenantIdGeneration generator, TenantIdMapping nested) {
		AttributeMapping leafAttribute() {
			return nested == null ? attribute : nested.leafAttribute();
		}

		void validate(Object owner, SharedSessionContractImplementor session) {
			final Object value = owner == null ? null : attribute.getValue( owner );
			if ( nested == null ) {
				generator.validateTenantId( session, value );
			}
			else {
				nested.validate( value, session );
			}
		}

		Object generate(Object value, Object entity, Type type, SharedSessionContractImplementor session) {
			if ( nested == null ) {
				return generator.generate( session, entity, value, INSERT );
			}
			final var componentType = (ComponentType) type;
			final Object[] values = value == null
					? new Object[componentType.getPropertyNames().length]
					: componentType.getPropertyValues( value, session );
			final int position = nested.attribute.getStateArrayPosition();
			values[position] = nested.generate( values[position], entity, componentType.getSubtypes()[position], session );
			return value == null
					? attribute.asEmbeddedAttributeMapping().getEmbeddableTypeDescriptor()
						.getRepresentationStrategy().getInstantiator().instantiate( () -> values )
					: componentType.replacePropertyValues( value, values, session );
		}
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
				bindings.bindValue( isRoot( session ) ? null : session.getTenantIdentifierValue(),
						selectable.getSelectionExpression(), ParameterUsage.TENANT );
			}
		}
	}

	public static boolean isRoot(SharedSessionContractImplementor session) {
		final var resolver = session.getFactory().getCurrentTenantIdentifierResolver();
		return resolver != null && resolver.isRoot( session.getTenantIdentifierValue() );
	}

	public static boolean needsMultiTableUpdateCheck(
			EntityPersister persister, SharedSessionContractImplementor session) {
		return persister.hasMultipleTables() && !isRoot( session ) && tenantIdMapping( persister ) != null;
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
		if ( id != null && persister.getGenerator() instanceof CompositeNestedGeneratedValueGenerator composite ) {
			final var type = (ComponentType) persister.getIdentifierType();
			for ( var plan : composite.getGenerationPlans() ) {
				if ( plan.getGenerator() instanceof TenantIdGeneration
						&& type.getPropertyValue( id, plan.getPropertyIndex(), session ) == null ) {
					return true;
				}
			}
		}
		return false;
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
		final var tenantMapping = tenantIdMapping( persister, persister.getGenerators() );
		if ( tenantMapping != null ) {
			tenantMapping.validate( entity, session );
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
		final var tenantMapping = tenantIdMapping( persister, persister.getGenerators() );
		if ( tenantMapping != null ) {
			final int position = tenantMapping.attribute.getStateArrayPosition();
			state[position] = tenantMapping.generate( state[position], entity, persister.getPropertyTypes()[position], session );
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
			final boolean lock = session.isTransactionInProgress()
					&& ( persister.hasMultipleTables() || persister.hasOwnedCollections() );
			if ( selectTenantId( id, persister, tenantMapping, true, lock, session ) == null
					&& ( !allowMissing || rowExists( id, persister, session ) ) ) {
				throw new StaleObjectStateException( persister.getEntityName(), id );
			}
		}
	}

	static Object getTenantId(Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		return selectTenantId( id, persister, tenantIdMapping( persister ), false, false, session );
	}

	private static Object selectTenantId(
			Object id, EntityPersister persister, AttributeMapping tenantMapping,
			boolean restrictTenant, boolean lock, SharedSessionContractImplementor session) {
		final var selectable = tenantMapping.getSelectable( 0 );
		final String tableName = persister.physicalTableNameForMutation( selectable );
		final var select = new SimpleSelect( session.getFactory() )
				.setTableName( tableName )
				.addColumn( selectable.getSelectionExpression() )
				.setLockMode( lock ? LockMode.PESSIMISTIC_WRITE : LockMode.NONE );
		for ( var table : persister.getTableMappings() ) {
			if ( tableName.equals( table.getTableName() ) ) {
				for ( var column : table.getKeyMapping().getKeyColumns() ) {
					select.addRestriction( column.getColumnName() );
				}
				break;
			}
		}
		if ( restrictTenant ) {
			select.addRestriction( selectable.getSelectionExpression() );
		}
		// Select only the current row when history shares the entity's physical table.
		if ( persister.getTemporalMapping() != null
				&& session.getFactory().getSessionFactoryOptions().getTemporalTableStrategy()
						== org.hibernate.temporal.TemporalTableStrategy.SINGLE_TABLE ) {
			select.addWhereToken( persister.getTemporalMapping().getEndingColumnMapping().getSelectionExpression() + " is null" );
		}
		final String sql = select.toStatementString();
		final var jdbcMapping = selectable.getJdbcMapping();
		final var coordinator = session.getJdbcCoordinator();
		final var resources = coordinator.getLogicalConnection().getResourceRegistry();
		try {
			final var statement = coordinator.getStatementPreparer().prepareStatement( sql );
			try {
				persister.getIdentifierType().nullSafeSet( statement, id, 1, session );
				if ( restrictTenant ) {
					jdbcMapping.getJdbcValueBinder().bind( statement,
							jdbcMapping.convertToRelationalValue( session.getTenantIdentifierValue() ),
							persister.getIdentifierMapping().getJdbcTypeCount() + 1, session );
				}
				final var resultSet = coordinator.getResultSetReturn().extract( statement, sql );
				try {
					return resultSet.next()
							? jdbcMapping.convertToDomainValue( jdbcMapping.getJdbcValueExtractor().extract( resultSet, 1, session ) )
							: null;
				}
				finally {
					resources.release( resultSet, statement );
				}
			}
			finally {
				resources.release( statement );
				coordinator.afterStatementExecution();
			}
		}
		catch ( SQLException e ) {
			throw session.getJdbcServices().getSqlExceptionHelper()
					.convert( e, "Could not check tenant ownership of " + persister.getEntityName(), sql );
		}
	}

	private static boolean rowExists(
			Object id, EntityPersister persister, SharedSessionContractImplementor session) {
		// A tenant-restricted query cannot distinguish an absent row from another tenant's row.
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
