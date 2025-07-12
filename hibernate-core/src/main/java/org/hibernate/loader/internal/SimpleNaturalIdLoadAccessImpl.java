/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityGraph;

import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.loader.LoaderLogging;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;

/**
 * @author Steve Ebersole
 */
public class SimpleNaturalIdLoadAccessImpl<T>
		extends BaseNaturalIdLoadAccessImpl<T>
		implements SimpleNaturalIdLoadAccess<T> {
	private final boolean hasSimpleNaturalId;

	public SimpleNaturalIdLoadAccessImpl(LoadAccessContext context, EntityMappingType entityDescriptor) {
		super( context, entityDescriptor );

		hasSimpleNaturalId = entityDescriptor.getNaturalIdMapping() instanceof SimpleNaturalIdMapping;

		if ( !hasSimpleNaturalId ) {
			// Just log it - we allow this for composite natural ids with the assumption
			// that a singular representation of the natural id (Map or array) will be passed
			LoaderLogging.LOADER_LOGGER.debugf(
					"Entity [%s] did not define a simple natural id",
					entityDescriptor.getEntityName()
			);
		}
	}

	@Override
	public LockOptions getLockOptions() {
		return super.getLockOptions();
	}

	@Override
	public boolean isSynchronizationEnabled() {
		return super.isSynchronizationEnabled();
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope) {
		//noinspection unchecked
		return (SimpleNaturalIdLoadAccess<T>) super.with( lockMode, lockScope );
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> with(Timeout timeout) {
		//noinspection unchecked
		return (SimpleNaturalIdLoadAccess<T>) super.with( timeout );
	}

	@Override
	public final SimpleNaturalIdLoadAccessImpl<T> with(LockOptions lockOptions) {
		return (SimpleNaturalIdLoadAccessImpl<T>) super.with( lockOptions );
	}

	@Override
	public SimpleNaturalIdLoadAccessImpl<T> setSynchronizationEnabled(boolean synchronizationEnabled) {
		super.synchronizationEnabled( synchronizationEnabled );
		return this;
	}

	@Override
	public T getReference(Object naturalIdValue) {
		verifySimplicity( naturalIdValue );
		return doGetReference( entityPersister().getNaturalIdMapping().normalizeInput( naturalIdValue) );
	}

	@Override
	public T load(Object naturalIdValue) {
		verifySimplicity( naturalIdValue );
		return doLoad( entityPersister().getNaturalIdMapping().normalizeInput( naturalIdValue) );
	}

	/**
	 * Verify that the given natural id is "simple".
	 * <p>
	 * We allow compound natural id "simple" loading if all the values are passed as an array,
	 * list, or map. We assume an array is properly ordered following the attribute ordering.
	 * For lists, just like arrays, we assume the user has ordered them properly; for maps,
	 * the key is expected to be the attribute name.
	 */
	private void verifySimplicity(Object naturalIdValue) {
		assert naturalIdValue != null;
		if ( !hasSimpleNaturalId
				&& !naturalIdValue.getClass().isArray()
				&& !(naturalIdValue instanceof List)
				&& !(naturalIdValue instanceof Map) ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Cannot interpret natural-id value [%s] for compound natural-id: %s",
							naturalIdValue,
							entityPersister().getEntityName()
					)
			);
		}
	}

	@Override
	public Optional<T> loadOptional(Object naturalIdValue) {
		return Optional.ofNullable( load( naturalIdValue ) );
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic) {
		super.with( graph, semantic );
		return this;
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> withLoadGraph(EntityGraph<T> graph) {
		return SimpleNaturalIdLoadAccess.super.withLoadGraph(graph);
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> enableFetchProfile(String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> disableFetchProfile(String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}
}
