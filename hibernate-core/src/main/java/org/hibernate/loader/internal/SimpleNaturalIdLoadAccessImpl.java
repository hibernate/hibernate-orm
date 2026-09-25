package org.hibernate.loader.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
import org.hibernate.internal.find.StatefulLoadAccessContext;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.internal.SimpleNaturalIdMapping;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Implementation of {@link SimpleNaturalIdLoadAccess}.
 *
 * @implNote We allow use of {@code SimpleNaturalIdLoadAccess} for
 * composite natural ids with the assumption that we will be given
 * a singular representation of the natural id (a map or array).
 *
 * @author Steve Ebersole
 */
public class SimpleNaturalIdLoadAccessImpl<T>
		extends BaseNaturalIdLoadAccessImpl<T>
		implements SimpleNaturalIdLoadAccess<T> {

	private final boolean hasSimpleNaturalId;

	public SimpleNaturalIdLoadAccessImpl(@Nonnull StatefulLoadAccessContext context, @Nonnull EntityMappingType entityDescriptor) {
		super( context, entityDescriptor );
		hasSimpleNaturalId = entityDescriptor.getNaturalIdMapping() instanceof SimpleNaturalIdMapping;
	}

	@Nullable
	@Override
	public LockOptions getLockOptions() {
		return super.getLockOptions();
	}

	@Override
	public boolean isSynchronizationEnabled() {
		return super.isSynchronizationEnabled();
	}

	@Nonnull
	@Override
	public SimpleNaturalIdLoadAccess<T> with(@Nonnull LockMode lockMode, @Nonnull PessimisticLockScope lockScope) {
		//noinspection unchecked
		return (SimpleNaturalIdLoadAccess<T>) super.with( lockMode, lockScope );
	}

	@Nonnull
	public SimpleNaturalIdLoadAccess<T> with(@Nonnull PessimisticLockScope lockScope) {
		super.with( lockScope );
		return this;
	}

	@Nonnull
	@Override
	public SimpleNaturalIdLoadAccess<T> with(@Nonnull Timeout timeout) {
		//noinspection unchecked
		return (SimpleNaturalIdLoadAccess<T>) super.with( timeout );
	}

	@Nonnull
	@Override
	public final SimpleNaturalIdLoadAccessImpl<T> with(@Nonnull LockOptions lockOptions) {
		return (SimpleNaturalIdLoadAccessImpl<T>) super.with( lockOptions );
	}

	@Nonnull
	@Override
	public SimpleNaturalIdLoadAccessImpl<T> setSynchronizationEnabled(boolean synchronizationEnabled) {
		super.synchronizationEnabled( synchronizationEnabled );
		return this;
	}

	@Nullable
	@Override
	public T getReference(@Nullable Object naturalIdValue) {
		verifySimplicity( naturalIdValue );
		return doGetReference( entityPersister().getNaturalIdMapping().normalizeInput( naturalIdValue) );
	}

	@Nullable
	@Override
	public T load(@Nullable Object naturalIdValue) {
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
	private void verifySimplicity(@Nullable Object naturalIdValue) {
		if ( !hasSimpleNaturalId
				&& ( naturalIdValue == null || !naturalIdValue.getClass().isArray() )
				&& !(naturalIdValue instanceof List)
				&& !(naturalIdValue instanceof Map)
				&& ! ( isNaturalIdClass( naturalIdValue ) ) ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Cannot interpret natural id value [%s] as compound natural id of entity '%s'",
							naturalIdValue,
							entityPersister().getEntityName()
					)
			);
		}
	}

	private boolean isNaturalIdClass(@Nullable Object naturalIdValue) {
		final EntityPersister entityPersister = entityPersister();
		final var naturalIdClass = entityPersister.getNaturalIdMapping().getNaturalIdClass();
		return naturalIdClass != null && naturalIdClass.isInstance( naturalIdValue );
	}

	@Nonnull
	@Override
	public Optional<T> loadOptional(@Nullable Object naturalIdValue) {
		return Optional.ofNullable( load( naturalIdValue ) );
	}

	@Nonnull
	@Override
	public SimpleNaturalIdLoadAccess<T> with(@Nonnull EntityGraph<T> graph, @Nonnull GraphSemantic semantic) {
		super.with( graph, semantic );
		return this;
	}

	@Nonnull
	@Override
	public SimpleNaturalIdLoadAccess<T> withLoadGraph(@Nonnull EntityGraph<T> graph) {
		return SimpleNaturalIdLoadAccess.super.withLoadGraph(graph);
	}

	@Nonnull
	@Override
	public SimpleNaturalIdLoadAccess<T> enableFetchProfile(@Nonnull String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}

	@Nonnull
	@Override
	public SimpleNaturalIdLoadAccess<T> disableFetchProfile(@Nonnull String profileName) {
		super.enableFetchProfile( profileName );
		return this;
	}
}
