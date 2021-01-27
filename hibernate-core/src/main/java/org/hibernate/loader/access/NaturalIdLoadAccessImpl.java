package org.hibernate.loader.access;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.LockOptions;
import org.hibernate.NaturalIdLoadAccess;
import org.hibernate.engine.spi.SessionImplementor;
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
	public NaturalIdLoadAccess<T> using(String attributeName, Object value) {
		naturalIdParameters.put( attributeName, value );
		return this;
	}

	@Override
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
		final SessionImplementor session = getContext().getSession();
		final Object normalizedValue = entityPersister().getNaturalIdMapping().normalizeInput( naturalIdParameters, session );

		return doGetReference( normalizedValue );
	}

	@Override
	public final T load() {
		final SessionImplementor session = getContext().getSession();
		final Object normalizedValue = entityPersister().getNaturalIdMapping().normalizeInput( naturalIdParameters, session );

		return doLoad( normalizedValue );
	}

	@Override
	public Optional<T> loadOptional() {
		return Optional.ofNullable( load() );
	}
}
