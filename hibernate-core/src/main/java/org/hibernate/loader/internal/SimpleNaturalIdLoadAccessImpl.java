/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.SimpleNaturalIdLoadAccess;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
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
			// just log it - we allow this for composite natural-ids with the assumption
			// that a singular representation of the natural-id (Map or array) will be passed
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

	private void verifySimplicity(Object naturalIdValue) {
		assert naturalIdValue != null;

		if ( hasSimpleNaturalId ) {
			// implicitly
			return;
		}

		if ( naturalIdValue.getClass().isArray() ) {
			// we allow compound natural-id "simple" loading all the values are passed as an array
			// (we assume the array is properly ordered following the mapping-model attribute ordering)
			return;
		}

		if ( naturalIdValue instanceof List || naturalIdValue instanceof Map ) {
			// also allowed.  For Lists, just like arrays, we assume the user has ordered them properly;
			// for Maps, the key is expected to be the attribute name
			return;
		}

		throw new HibernateException(
				String.format(
						Locale.ROOT,
						"Cannot interpret natural-id value [%s] for compound natural-id: %s",
						naturalIdValue,
						entityPersister().getEntityName()
				)
		);
	}

	@Override
	public Optional<T> loadOptional(Object naturalIdValue) {
		return Optional.ofNullable( load( naturalIdValue ) );
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic) {
		super.with( graph, semantic );
		return this;
	}

	@Override
	public SimpleNaturalIdLoadAccess<T> withLoadGraph(RootGraph<T> graph) {
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
