/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.action.internal.CollectionRemoveAction;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.internal.FlushProcessingContext;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.event.internal.EventListenerLogging.EVENT_LISTENER_LOGGER;
import static org.hibernate.persister.entity.AbstractEntityPersister.getCollectionKey;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Wrap collections in {@linkplain PersistentCollection collection wrappers}.
 *
 * @author Gavin King
 */
public class WrapVisitor extends ProxyVisitor {

	protected Object entity;
	protected Object id;

	private boolean substitute;

	public WrapVisitor(@Nonnull Object entity, @Nullable Object id, @Nonnull EventSource session) {
		super( session );
		this.entity = entity;
		this.id = id;
	}

	public boolean isSubstitutionRequired() {
		return substitute;
	}

	@Override
	@Nullable
	protected Object processCollection(@Nullable Object collection, @Nonnull CollectionType collectionType)
			throws HibernateException {
		if ( collection == null || collection == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			return null;
		}
		else if ( collection instanceof PersistentCollection<?> persistentCollection ) {
			if ( persistentCollection.setCurrentSession( getSession() ) ) {
				reattachCollection( persistentCollection, collectionType );
			}
			return null;
		}
		else {
			return processArrayOrNewCollection( collection, collectionType );
		}
	}

	@Nullable
	final Object processArrayOrNewCollection(@Nullable Object collection, @Nonnull CollectionType collectionType)
			throws HibernateException {
		if ( collection == null ) {
			//do nothing
			return null;
		}
		else {
			final var session = getSession();
			final var persister =
					session.getFactory().getMappingMetamodel()
							.getCollectionDescriptor( collectionType.getRole() );
			final var persistenceContext = session.getPersistenceContextInternal();
			//TODO: move into collection type, so we can use polymorphism!
			if ( collectionType.hasHolder() ) {
				if ( persistenceContext.getCollectionHolder( collection ) == null ) {
					final var collectionHolder = collectionType.wrap( session, collection );
					persistenceContext.addNewCollection( persister, collectionHolder );
					persistenceContext.addCollectionHolder( collectionHolder );
				}
				return null;
			}
			else {
				if ( isPersistentAttributeInterceptable( entity ) ) {
					final var attributeInterceptor =
							asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
					if ( attributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
						return null;
					}
					else if ( attributeInterceptor instanceof LazyAttributeLoadingInterceptor lazyLoadingInterceptor ) {
						if ( lazyLoadingInterceptor.isAttributeLoaded( persister.getAttributeMapping().getAttributeName() ) ) {
							final var entry = persistenceContext.getEntry( entity );
							if ( entry.isExistsInDatabase() ) {
								scheduleRemoval( persister, entry, session, persistenceContext );
							}
						}
					}
				}

				final var persistentCollection = collectionType.wrap( session, collection );
				persistenceContext.addNewCollection( persister, persistentCollection );
				if ( EVENT_LISTENER_LOGGER.isTraceEnabled() ) {
					EVENT_LISTENER_LOGGER.wrappedCollectionInRole( collectionType.getRole() );
				}
				return persistentCollection; //Force a substitution!
			}
		}
	}

	private void scheduleRemoval(
			@Nonnull CollectionPersister persister,
			@Nonnull EntityEntry entry,
			@Nonnull EventSource session,
			@Nonnull PersistenceContext persistenceContext) {
		final Object key = getCollectionKey( persister, entity, entry, session );
		if ( key != null ) {
			final var existing = persistenceContext.getCollection( new CollectionKey( persister, key ) );
			if ( existing == null ) {
				// the collection has not been initialized and new collection values have been assigned,
				// we need to be sure to delete all the collection elements before inserting the new ones
				final var collection =
						persister.getCollectionSemantics()
								.instantiateWrapper( key, persister, session );
				persistenceContext.addUninitializedCollection(
						persister,
						collection,
						key,
						entry.isReadOnly()
				);
				scheduleRemoval( persister, session, persistenceContext, collection, key );
			}
		}
	}

	private static void scheduleRemoval(
			@NotNull CollectionPersister persister,
			@NotNull EventSource session,
			@NotNull PersistenceContext persistenceContext,
			@NotNull PersistentCollection<?> collectionToRemove,
			@NotNull Object key) {
		if ( persistenceContext.getCollectionFlushActionTracker()
				instanceof FlushProcessingContext flushProcessingContext ) {
			flushProcessingContext.queueCollectionRemove(
					collectionToRemove,
					persister,
					key,
					false
			);
		}
		else {
			session.runInterceptorCallback(
					() -> session.getInterceptor().onCollectionRemove( collectionToRemove, key ) );
			session.getActionQueue().addAction(
					new CollectionRemoveAction(
							collectionToRemove,
							persister,
							key,
							false,
							session
					)
			);
		}
	}

	@Override
	protected void processValue(int i, @Nonnull Object[] values, @Nonnull Type[] types) {
		final Object result = processValue( values[i], types[i] );
		if ( result != null ) {
			substitute = true;
			values[i] = result;
		}
	}

	@Override
	@Nullable
	protected Object processComponent(@Nullable Object component, @Nonnull CompositeType compositeType) {
		if ( component != null ) {
			final Object[] values = compositeType.getPropertyValues( component, getSession() );
			final Type[] types = compositeType.getSubtypes();
			boolean substituteComponent = false;
			for ( int i = 0; i < types.length; i++ ) {
				final Object result = processValue( values[i], types[i] );
				if ( result != null ) {
					values[i] = result;
					substituteComponent = true;
				}
			}
			if ( substituteComponent ) {
				final Object newComponent = compositeType.replacePropertyValues( component, values, getSession() );
				return newComponent == component ? null : newComponent;
			}
		}

		return null;
	}

	@Override
	public void process(@Nonnull Object object, @Nonnull EntityPersister persister) {
		final Object[] values = persister.getValues( object );
		final Type[] types = persister.getPropertyTypes();
		processEntityPropertyValues( values, types );
		if ( isSubstitutionRequired() ) {
			persister.setValues( object, values );
		}
	}
}
