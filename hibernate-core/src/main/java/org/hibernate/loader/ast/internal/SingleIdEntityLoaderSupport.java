package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

import static org.hibernate.binder.internal.TenantIdBinder.FILTER_NAME;
import static org.hibernate.binder.internal.TenantIdBinder.PARAMETER_NAME;

/**
 * @author Steve Ebersole
 */
public abstract class SingleIdEntityLoaderSupport<T> implements SingleIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	protected final SessionFactoryImplementor sessionFactory;

	@Nullable
	private DatabaseSnapshotExecutor databaseSnapshotExecutor;
	@Nullable
	private DatabaseSnapshotExecutor tenantDatabaseSnapshotExecutor;

	public SingleIdEntityLoaderSupport(@Nonnull EntityMappingType entityDescriptor, @Nonnull SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;
	}

	@Nonnull
	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Nullable
	@Override
	public Object[] loadDatabaseSnapshot(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		final var tenantFilter = session.getLoadQueryInfluencers().getEnabledFilter( FILTER_NAME );
		if ( tenantFilter != null ) {
			if ( tenantDatabaseSnapshotExecutor == null ) {
				tenantDatabaseSnapshotExecutor = new DatabaseSnapshotExecutor( entityDescriptor, sessionFactory, tenantFilter );
			}
			return tenantDatabaseSnapshotExecutor.loadDatabaseSnapshot(
					id, tenantFilter.getParameterValue( PARAMETER_NAME ), session );
		}
		if ( databaseSnapshotExecutor == null ) {
			databaseSnapshotExecutor = new DatabaseSnapshotExecutor( entityDescriptor, sessionFactory );
		}
		return databaseSnapshotExecutor.loadDatabaseSnapshot( id, null, session );
	}
}
