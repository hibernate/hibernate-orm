/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.internal.SimpleQueryOptions;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.Callback;

/**
 * @author Steve Ebersole
 */
class SingleIdExecutionContext extends BaseExecutionContext {
	private final Object entityInstance;
	private final Object entityId;
	private final EntityMappingType rootEntityDescriptor;
	private final Boolean readOnly;
	private final LockOptions lockOptions;
	private final SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler;
	private final Callback callback;

	public SingleIdExecutionContext(
			Object entityId,
			Object entityInstance,
			EntityMappingType rootEntityDescriptor,
			Boolean readOnly,
			LockOptions lockOptions,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler,
			SharedSessionContractImplementor session) {
		this(
				entityId,
				entityInstance,
				rootEntityDescriptor,
				readOnly,
				lockOptions,
				subSelectFetchableKeysHandler,
				session,
				null
		);
	}

	public SingleIdExecutionContext(
			Object entityId,
			Object entityInstance,
			EntityMappingType rootEntityDescriptor,
			Boolean readOnly,
			LockOptions lockOptions,
			SubselectFetch.RegistrationHandler subSelectFetchableKeysHandler,
			SharedSessionContractImplementor session,
			Callback callback) {
		super( session );
		this.entityInstance = entityInstance;
		this.entityId = entityId;
		this.rootEntityDescriptor = rootEntityDescriptor;
		this.readOnly = readOnly;
		this.lockOptions = lockOptions;
		this.subSelectFetchableKeysHandler = subSelectFetchableKeysHandler;
		this.callback = callback;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public Object getEntityId() {
		return entityId;
	}

	@Override
	public EntityMappingType getRootEntityDescriptor() {
		return rootEntityDescriptor;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return new SimpleQueryOptions( lockOptions, readOnly );
	}

	@Override
	public void registerLoadingEntityHolder(EntityHolder holder) {
		subSelectFetchableKeysHandler.addKey( holder );
	}

	@Override
	public Callback getCallback() {
		return callback;
	}
}
