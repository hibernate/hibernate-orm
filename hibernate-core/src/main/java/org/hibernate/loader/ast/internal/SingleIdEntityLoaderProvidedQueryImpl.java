/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import jakarta.persistence.QueryFlushMode;
import org.hibernate.query.named.spi.NamedQueryMemento;

import jakarta.persistence.Parameter;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_OBJECT_ARRAY;
import static org.hibernate.query.ResultListTransformer.uniqueResultTransformer;

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
			@Nonnull EntityMappingType entityDescriptor,
			@Nonnull NamedQueryMemento<T> namedQueryMemento) {
		this.entityDescriptor = entityDescriptor;
		this.namedQueryMemento = namedQueryMemento;
	}

	@Nonnull
	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Nullable
	@Override @SuppressWarnings("unchecked")
	public T load(@Nonnull Object pkValue, @Nonnull LockOptions lockOptions, @Nullable Boolean readOnly, @Nonnull SharedSessionContractImplementor session) {
		final var mappedJavaType = (JavaType<T>) entityDescriptor.getMappedJavaType();
		final var query = namedQueryMemento.toSelectionQuery( session, mappedJavaType.getJavaTypeClass() );
		query.setParameter( (Parameter<Object>) query.getParameters().iterator().next(), pkValue );
		query.setQueryFlushMode( QueryFlushMode.NO_FLUSH );
		query.setResultListTransformer( uniqueResultTransformer() );
		return query.uniqueResult();
	}

	@Nullable
	@Override
	public T load(
			@Nonnull Object pkValue,
			@Nullable Object entityInstance,
			@Nonnull LockOptions lockOptions,
			@Nullable Boolean readOnly,
			@Nonnull SharedSessionContractImplementor session) {
		if ( entityInstance != null ) {
			throw new UnsupportedOperationException("null entity instance");
		}
		return load( pkValue, lockOptions, readOnly, session );
	}

	@Nullable
	@Override
	public Object[] loadDatabaseSnapshot(@Nonnull Object id, @Nonnull SharedSessionContractImplementor session) {
		return EMPTY_OBJECT_ARRAY;
	}
}
