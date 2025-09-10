/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.op;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.spi.Callback;

/**
 * @author Steve Ebersole
 */
class SingleIdExecutionContext extends BaseExecutionContext {
	private final Object entityInstance;
	private final Object restrictedValue;
	private final EntityMappingType rootEntityDescriptor;
	private final QueryOptions queryOptions;
	private final Callback callback;

	public SingleIdExecutionContext(
			SharedSessionContractImplementor session,
			Object entityInstance,
			Object restrictedValue,
			EntityMappingType rootEntityDescriptor, QueryOptions queryOptions,
			Callback callback) {
		super( session );
		this.entityInstance = entityInstance;
		this.restrictedValue = restrictedValue;
		this.rootEntityDescriptor = rootEntityDescriptor;
		this.queryOptions = queryOptions;
		this.callback = callback;
	}

	@Override
	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public Object getEntityId() {
		return restrictedValue;
	}

	@Override
	public EntityMappingType getRootEntityDescriptor() {
		return rootEntityDescriptor;
	}

	@Override
	public QueryOptions getQueryOptions() {
		return queryOptions;
	}

	@Override
	public Callback getCallback() {
		return callback;
	}

}
