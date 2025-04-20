/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryImplementor;

import jakarta.persistence.Parameter;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_OBJECT_ARRAY;

/**
 * Implementation of SingleIdEntityLoader for cases where the application has
 * provided the select load query
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderProvidedQueryImpl<T> implements SingleIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final NamedQueryMemento<T> namedQueryMemento;

	public SingleIdEntityLoaderProvidedQueryImpl(
			EntityMappingType entityDescriptor,
			NamedQueryMemento<T> namedQueryMemento) {
		this.entityDescriptor = entityDescriptor;
		this.namedQueryMemento = namedQueryMemento;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override @SuppressWarnings("unchecked")
	public T load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		final JavaType<T> mappedJavaType = (JavaType<T>) entityDescriptor.getMappedJavaType();
		final QueryImplementor<T> query = namedQueryMemento.toQuery( session, mappedJavaType.getJavaTypeClass() );
		query.setParameter( (Parameter<Object>) query.getParameters().iterator().next(), pkValue );
		query.setQueryFlushMode( QueryFlushMode.NO_FLUSH );
		return query.uniqueResult();
	}

	@Override
	public T load(
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		if ( entityInstance != null ) {
			throw new UnsupportedOperationException("null entity instance");
		}
		return load( pkValue, lockOptions, readOnly, session );
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		return EMPTY_OBJECT_ARRAY;
	}
}
