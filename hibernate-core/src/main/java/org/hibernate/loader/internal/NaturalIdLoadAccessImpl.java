/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.internal.find.StatefulLoadAccessContext;
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

	public NaturalIdLoadAccessImpl(@Nonnull StatefulLoadAccessContext context, @Nonnull EntityMappingType entityDescriptor) {
		super( context, entityDescriptor );
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccess<T> with(@Nonnull LockMode lockMode, @Nonnull PessimisticLockScope lockScope) {
		//noinspection unchecked
		return (NaturalIdLoadAccess<T>) super.with( lockMode, lockScope );
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccess<T> with(@Nonnull Timeout timeout) {
		//noinspection unchecked
		return (NaturalIdLoadAccess<T>) super.with( timeout );
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccessImpl<T> with(@Nonnull LockOptions lockOptions) {
		return (NaturalIdLoadAccessImpl<T>) super.with( lockOptions );
	}

	@Nonnull
	@Override
	public <X> NaturalIdLoadAccess<T> using(@Nonnull SingularAttribute<? super T, X> attribute, @Nonnull X value) {
		naturalIdParameters.put( attribute.getName(), value );
		return this;
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccess<T> using(@Nonnull String attributeName, @Nonnull Object value) {
		naturalIdParameters.put( attributeName, value );
		return this;
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccess<T> using(@Nonnull Map<String, ?> mappings) {
		naturalIdParameters.putAll( mappings );
		return this;
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccessImpl<T> setSynchronizationEnabled(boolean synchronizationEnabled) {
		super.synchronizationEnabled( synchronizationEnabled );
		return this;
	}

	@Nullable
	@Override
	public final T getReference() {
		return doGetReference( entityPersister().getNaturalIdMapping().normalizeInput( naturalIdParameters ) );
	}

	@Nullable
	@Override
	public final T load() {
		return doLoad( entityPersister().getNaturalIdMapping().normalizeInput( naturalIdParameters ) );
	}

	@Nonnull
	@Override
	public Optional<T> loadOptional() {
		return Optional.ofNullable( load() );
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccess<T> with(@Nonnull EntityGraph<T> graph, @Nonnull GraphSemantic semantic) {
		super.with( graph, semantic );
		return this;
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccess<T> enableFetchProfile(@Nonnull String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Nonnull
	@Override
	public NaturalIdLoadAccess<T> disableFetchProfile(@Nonnull String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}
}
