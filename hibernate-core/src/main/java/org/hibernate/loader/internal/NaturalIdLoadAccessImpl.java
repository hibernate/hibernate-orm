/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.metamodel.mapping.EntityMappingType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link NaturalIdLoadAccess}.
 *
 * @author Steve Ebersole
 */
public class NaturalIdLoadAccessImpl<T>
		extends BaseNaturalIdLoadAccessImpl<T>
		implements NaturalIdLoadAccess<T> {

	private final Map<String, Object> naturalIdParameters = new LinkedHashMap<>();

	public NaturalIdLoadAccessImpl(LoadAccessContext context, EntityMappingType entityDescriptor) {
		super( context, entityDescriptor );
	}

	@Override
	public NaturalIdLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope) {
		//noinspection unchecked
		return (NaturalIdLoadAccess<T>) super.with( lockMode, lockScope );
	}

	@Override
	public NaturalIdLoadAccess<T> with(Timeout timeout) {
		//noinspection unchecked
		return (NaturalIdLoadAccess<T>) super.with( timeout );
	}

	@Override
	public NaturalIdLoadAccessImpl<T> with(LockOptions lockOptions) {
		return (NaturalIdLoadAccessImpl<T>) super.with( lockOptions );
	}

	@Override
	public <X> NaturalIdLoadAccess<T> using(SingularAttribute<? super T, X> attribute, X value) {
		naturalIdParameters.put( attribute.getName(), value );
		return this;
	}

	@Override
	public NaturalIdLoadAccess<T> using(String attributeName, Object value) {
		naturalIdParameters.put( attributeName, value );
		return this;
	}

	@Override
	public NaturalIdLoadAccess<T> using(Map<String, ?> mappings) {
		naturalIdParameters.putAll( mappings );
		return this;
	}

	@Override
	public NaturalIdLoadAccessImpl<T> setSynchronizationEnabled(boolean synchronizationEnabled) {
		super.synchronizationEnabled( synchronizationEnabled );
		return this;
	}

	@Override
	public final T getReference() {
		return doGetReference( entityPersister().getNaturalIdMapping().normalizeInput( naturalIdParameters ) );
	}

	@Override
	public final T load() {
		return doLoad( entityPersister().getNaturalIdMapping().normalizeInput( naturalIdParameters ) );
	}

	@Override
	public Optional<T> loadOptional() {
		return Optional.ofNullable( load() );
	}

	@Override
	public NaturalIdLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic) {
		super.with( graph, semantic );
		return this;
	}

	@Override
	public NaturalIdLoadAccess<T> enableFetchProfile(String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Override
	public NaturalIdLoadAccess<T> disableFetchProfile(String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}
}
