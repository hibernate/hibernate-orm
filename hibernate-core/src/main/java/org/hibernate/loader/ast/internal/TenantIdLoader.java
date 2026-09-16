/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.SimpleSelect;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.exec.spi.JdbcParametersList;

/**
 * Stored tenant ownership, independent of application filters and provided entity loaders.
 * The locking query addresses the physical tenant table directly, avoiding follow-on locking.
 */
public class TenantIdLoader {
	private final EntityPersister persister;
	private final String ownerSql;
	private final String tenantSql;
	private final String lockedTenantSql;
	private final SingleIdArrayLoadPlan existencePlan;
	private final DatabaseSnapshotExecutor cacheInvalidationPlan;

	public TenantIdLoader(EntityPersister persister) {
		this.persister = persister;
		ownerSql = createSql( false, false );
		tenantSql = createSql( true, false );
		lockedTenantSql = createSql( true, true );
		existencePlan = createLoadPlan( List.of( persister.getIdentifierMapping() ) );
		final var tenantAttribute = persister.getTenantIdMapping().getAttributeMapping();
		cacheInvalidationPlan = persister.canWriteToCache() && persister.isVersioned()
				? new DatabaseSnapshotExecutor( persister, persister.getFactory(), null,
						List.of( tenantAttribute, persister.getVersionMapping() ) ) : null;
	}

	public Object loadTenantId(Object id, SharedSessionContractImplementor session) {
		return selectTenantId( id, false, false, session );
	}

	public boolean belongsToTenant(Object id, boolean lock, SharedSessionContractImplementor session) {
		return selectTenantId( id, true, lock, session ) != null;
	}

	public boolean rowExists(Object id, SharedSessionContractImplementor session) {
		// A tenant-restricted query cannot distinguish an absent row from another tenant's row.
		return existencePlan.load( id, session ) != null;
	}

	/**
	 * Read only the stored owner and cache version, never detached entity state.
	 */
	public Snapshot loadCacheSnapshot(Object id, SharedSessionContractImplementor session) {
		if ( cacheInvalidationPlan == null ) {
			return new Snapshot( loadTenantId( id, session ), null );
		}
		final Object[] row = cacheInvalidationPlan.loadDatabaseSnapshot( id, null, session );
		return row == null ? new Snapshot( null, null ) : new Snapshot( row[0], row[1] );
	}

	public record Snapshot(Object tenantId, Object version) {}

	private SingleIdArrayLoadPlan createLoadPlan(List<? extends ModelPart> parts) {
		final var factory = persister.getFactory();
		final var identifier = persister.getIdentifierMapping();
		final var parameters = JdbcParametersList.newBuilder();
		final var select = LoaderSelectBuilder.createSelect(
				persister, parts, identifier, null, 1,
				new LoadQueryInfluencers( factory ), LockOptions.NONE, parameters::add,
				new SqlAliasBaseManager(), factory
		);
		return new SingleIdArrayLoadPlan( persister, identifier, select, parameters.build(), LockOptions.NONE, factory );
	}

	private String createSql(boolean restrictTenant, boolean lock) {
		final var selectable = persister.getTenantIdMapping().getAttributeMapping().getSelectable( 0 );
		final String tableName = persister.physicalTableNameForMutation( selectable );
		final var select = new SimpleSelect( persister.getFactory() )
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
				&& persister.getFactory().getSessionFactoryOptions().getTemporalTableStrategy()
						== org.hibernate.temporal.TemporalTableStrategy.SINGLE_TABLE ) {
			select.addWhereToken( persister.getTemporalMapping().getEndingColumnMapping().getSelectionExpression() + " is null" );
		}
		return select.toStatementString();
	}

	private Object selectTenantId(
			Object id, boolean restrictTenant, boolean lock, SharedSessionContractImplementor session) {
		final String sql = restrictTenant ? lock ? lockedTenantSql : tenantSql : ownerSql;
		final var jdbcMapping = persister.getTenantIdMapping().getAttributeMapping().getSelectable( 0 ).getJdbcMapping();
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
}
