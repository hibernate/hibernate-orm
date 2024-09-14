/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * @author Steve Ebersole
 */
public class NaturalIdLoadAccessImpl<T> extends BaseNaturalIdLoadAccessImpl<T> implements NaturalIdLoadAccess<T> {
	private final Map<String, Object> naturalIdParameters = new LinkedHashMap<>();

	public NaturalIdLoadAccessImpl(LoadAccessContext context, EntityMappingType entityDescriptor) {
		super( context, entityDescriptor );
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

	@Override @Deprecated
	public NaturalIdLoadAccess<T> using(Object... mappings) {
		CollectionHelper.collectMapEntries( naturalIdParameters::put, mappings );
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
	public NaturalIdLoadAccess<T> with(RootGraph<T> graph, GraphSemantic semantic) {
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
