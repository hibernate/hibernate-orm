/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.loader.ast.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.loader.ast.internal.CacheEntityLoaderHelper.PersistenceContextEntry;
import org.hibernate.loader.ast.spi.MultiIdEntityLoader;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;

import org.checkerframework.checker.nullness.qual.NonNull;

import static org.hibernate.internal.util.collections.CollectionHelper.isEmpty;

/**
 * Base support for MultiIdEntityLoader implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractMultiIdEntityLoader<T> implements MultiIdEntityLoader<T>, Preparable {
	private final EntityMappingType entityDescriptor;
	private final SessionFactoryImplementor sessionFactory;

	private EntityIdentifierMapping identifierMapping;

	public AbstractMultiIdEntityLoader(EntityMappingType entityDescriptor, SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void prepare() {
		identifierMapping = getLoadable().getIdentifierMapping();
	}

	protected EntityMappingType getEntityDescriptor() {
		return entityDescriptor;
	}

	protected SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}

	public EntityIdentifierMapping getIdentifierMapping() {
		return identifierMapping;
	}

	@Override
	public EntityMappingType getLoadable() {
		return getEntityDescriptor();
	}

	@Override
	public final <K> List<T> load(K[] ids, MultiIdLoadOptions loadOptions, EventSource session) {
		assert ids != null;
		if ( loadOptions.isOrderReturnEnabled() ) {
			return performOrderedMultiLoad( ids, loadOptions, session );
		}
		else {
			return performUnorderedMultiLoad( ids, loadOptions, session );
		}
	}

	protected abstract <K> List<T> performOrderedMultiLoad(K[] ids, MultiIdLoadOptions loadOptions, EventSource session);

	protected abstract <K> List<T> performUnorderedMultiLoad(K[] ids, MultiIdLoadOptions loadOptions, EventSource session);

	public interface ResolutionConsumer<T> {
		void consume(int position, EntityKey entityKey, T resolvedRef);
	}

	protected final <K> K[] processResolvableEntities(
			K[] ids,
			ResolutionConsumer<T> resolutionConsumer,
			@NonNull MultiIdLoadOptions loadOptions,
			@NonNull LockOptions lockOptions,
			EventSource session) {
		if ( !loadOptions.isSessionCheckingEnabled()
				&& !loadOptions.isSecondLevelCacheCheckingEnabled() ) {
			return ids;
		}

		final boolean coerce = !getSessionFactory().getJpaMetamodel().getJpaCompliance().isLoadByIdComplianceEnabled();

		boolean foundAnyResolvedEntities = false;
		List<K> nonResolvedIds = null;

		for ( int i = 0; i < ids.length; i++ ) {
			final Object id;
			if ( coerce ) {
				//noinspection unchecked
				id = (K) getLoadable().getIdentifierMapping().getJavaType().coerce( ids[i], session );
			}
			else {
				id = ids[i];
			}

			final EntityKey entityKey = new EntityKey( id, getLoadable().getEntityPersister() );
			final LoadEvent loadEvent = new LoadEvent(
					id,
					getLoadable().getJavaType().getJavaTypeClass().getName(),
					lockOptions,
					session,
					getReadOnlyFromLoadQueryInfluencers( session )
			);

			Object resolvedEntity = null;

			// look for it in the Session first
			final PersistenceContextEntry persistenceContextEntry = CacheEntityLoaderHelper.loadFromSessionCacheStatic(
					loadEvent,
					entityKey,
					LoadEventListener.GET
			);
			if ( loadOptions.isSessionCheckingEnabled() ) {
				resolvedEntity = persistenceContextEntry.getEntity();

				if ( resolvedEntity != null
						&& !loadOptions.isReturnOfDeletedEntitiesEnabled()
						&& !persistenceContextEntry.isManaged() ) {
					foundAnyResolvedEntities = true;
					resolutionConsumer.consume( i, entityKey, null );
					continue;
				}
			}

			if ( resolvedEntity == null && loadOptions.isSecondLevelCacheCheckingEnabled() ) {
				resolvedEntity = CacheEntityLoaderHelper.INSTANCE.loadFromSecondLevelCache(
						loadEvent,
						getLoadable().getEntityPersister(),
						entityKey
				);
			}

			if ( resolvedEntity != null ) {
				foundAnyResolvedEntities = true;

				//noinspection unchecked
				resolutionConsumer.consume( i, entityKey, (T) resolvedEntity );
			}
			else {
				if ( nonResolvedIds == null ) {
					nonResolvedIds = new ArrayList<>();
				}
				//noinspection unchecked,CastCanBeRemovedNarrowingVariableType
				nonResolvedIds.add( (K) id );
			}
		}

		if ( foundAnyResolvedEntities ) {
			if ( isEmpty( nonResolvedIds ) ) {
				// all the given ids were already associated with the Session
				return null;
			}

			return nonResolvedIds.toArray( createTypedArray(0) );
		}

		return ids;
	}

	protected <X> X[] createTypedArray(@SuppressWarnings("SameParameterValue") int length) {
		//noinspection unchecked
		return (X[]) new Object[length];
	}

	protected static Boolean getReadOnlyFromLoadQueryInfluencers(SharedSessionContractImplementor session) {
		Boolean readOnly = null;
		final LoadQueryInfluencers loadQueryInfluencers = session.getLoadQueryInfluencers();
		if ( loadQueryInfluencers != null ) {
			readOnly = loadQueryInfluencers.getReadOnly();
		}
		return readOnly;
	}
}
