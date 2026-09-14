/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;

import static org.hibernate.binder.internal.TenantIdBinder.FILTER_NAME;

/**
 * @author Steve Ebersole
 */
public abstract class SingleIdEntityLoaderSupport<T> implements SingleIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	protected final SessionFactoryImplementor sessionFactory;

	private DatabaseSnapshotExecutor databaseSnapshotExecutor;

	public SingleIdEntityLoaderSupport(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		final var tenantFilter = session.getLoadQueryInfluencers().getEnabledFilter( FILTER_NAME );
		if ( tenantFilter != null ) {
			// Filter parameter values are captured in the SQL operation, so this
			// executor must not be shared with sessions belonging to other tenants.
			return new DatabaseSnapshotExecutor( entityDescriptor, sessionFactory, tenantFilter )
					.loadDatabaseSnapshot( id, session );
		}
		if ( databaseSnapshotExecutor == null ) {
			databaseSnapshotExecutor = new DatabaseSnapshotExecutor( entityDescriptor, sessionFactory );
		}
		return databaseSnapshotExecutor.loadDatabaseSnapshot( id, session );
	}
}
