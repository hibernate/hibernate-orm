/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.internal;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptable;
import static org.hibernate.engine.internal.ManagedTypeHelper.isPersistentAttributeInterceptable;
import static org.hibernate.persister.entity.AbstractEntityPersister.getCollectionKey;

/**
 * Wrap collections in a Hibernate collection wrapper.
 *
 * @author Gavin King
 */
public class WrapVisitor extends ProxyVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( WrapVisitor.class );
	protected Object entity;
	protected Object id;

	private boolean substitute;

	public WrapVisitor(Object entity, Object id, EventSource session) {
		super( session );
		this.entity = entity;
		this.id = id;
	}

	public boolean isSubstitutionRequired() {
		return substitute;
	}

	@Override
	protected Object processCollection(Object entity, Object collection, CollectionType collectionType)
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

	final Object processArrayOrNewCollection(Object collection, CollectionType collectionType)
			throws HibernateException {
		if ( collection == null ) {
			//do nothing
			return null;
		}
		else {
			final SessionImplementor session = getSession();
			final CollectionPersister persister =
					session.getFactory().getMappingMetamodel()
							.getCollectionDescriptor( collectionType.getRole() );
			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			//TODO: move into collection type, so we can use polymorphism!
			if ( collectionType.hasHolder() ) {
				if ( collection != CollectionType.UNFETCHED_COLLECTION ) {
					if ( persistenceContext.getCollectionHolder( collection ) == null ) {
						final PersistentCollection<?> collectionHolder = collectionType.wrap( session, collection );
						persistenceContext.addNewCollection( persister, collectionHolder );
						persistenceContext.addCollectionHolder( collectionHolder );
					}
				}
				return null;
			}
			else {
				if ( isPersistentAttributeInterceptable( entity ) ) {
					final PersistentAttributeInterceptor attributeInterceptor =
							asPersistentAttributeInterceptable( entity ).$$_hibernate_getInterceptor();
					if ( attributeInterceptor instanceof EnhancementAsProxyLazinessInterceptor ) {
						return null;
					}
					else if ( attributeInterceptor != null
							&& ((LazyAttributeLoadingInterceptor) attributeInterceptor)
									.isAttributeLoaded( persister.getAttributeMapping().getAttributeName() ) ) {
						final EntityEntry entry = persistenceContext.getEntry( entity );
						if ( entry.isExistsInDatabase() ) {
							final Object key = getCollectionKey( persister, entity, entry, session );
							if ( key != null ) {
								PersistentCollection<?> collectionInstance =
										persistenceContext.getCollection( new CollectionKey( persister, key ) );
								if ( collectionInstance == null ) {
									// the collection has not been initialized and new collection values have been assigned,
									// we need to be sure to delete all the collection elements before inserting the new ones
									collectionInstance =
											persister.getCollectionSemantics()
													.instantiateWrapper( key, persister, session );
									persistenceContext.addUninitializedCollection( persister, collectionInstance, key );
									final CollectionEntry collectionEntry =
											persistenceContext.getCollectionEntry( collectionInstance );
									collectionEntry.setDoremove( true );
								}
							}
						}
					}
				}

				final PersistentCollection<?> persistentCollection = collectionType.wrap( session, collection );
				persistenceContext.addNewCollection( persister, persistentCollection );
				if ( LOG.isTraceEnabled() ) {
					LOG.trace( "Wrapped collection in role: " + collectionType.getRole() );
				}
				return persistentCollection; //Force a substitution!
			}
		}
	}

	@Override
	protected void processValue(Object entity, int i, Object[] values, Type[] types) {
		final Object result = processValue( values[i], values[i], types[i], i);
		if ( result != null ) {
			substitute = true;
			values[i] = result;
		}
	}

	@Override
	protected Object processComponent(Object entity, Object component, CompositeType componentType) throws HibernateException {
		if ( component != null ) {
			final Object[] values = componentType.getPropertyValues( component, getSession() );
			final Type[] types = componentType.getSubtypes();
			boolean substituteComponent = false;
			for ( int i = 0; i < types.length; i++ ) {
				Object result = processValue( values[i], values[i], types[i], i );
				if ( result != null ) {
					values[i] = result;
					substituteComponent = true;
				}
			}
			if ( substituteComponent ) {
				componentType.setPropertyValues( component, values );
			}
		}

		return null;
	}

	@Override
	public void process(Object object, EntityPersister persister) throws HibernateException {
		final Object[] values = persister.getValues( object );
		final Type[] types = persister.getPropertyTypes();
		processEntityPropertyValues( object, values, types );
		if ( isSubstitutionRequired() ) {
			persister.setValues( object, values );
		}
	}
}
