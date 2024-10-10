/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal;

import java.io.Serializable;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.metamodel.Attribute;

import org.hibernate.Hibernate;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.internal.util.PersistenceUtilHelper;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.LazyInitializer;

import org.jboss.logging.Logger;

import static jakarta.persistence.spi.LoadState.NOT_LOADED;
import static org.hibernate.engine.internal.ManagedTypeHelper.asManagedEntity;
import static org.hibernate.engine.internal.ManagedTypeHelper.isManagedEntity;
import static org.hibernate.jpa.internal.util.PersistenceUtilHelper.getLoadState;
import static org.hibernate.jpa.internal.util.PersistenceUtilHelper.isLoadedWithReference;
import static org.hibernate.jpa.internal.util.PersistenceUtilHelper.isLoadedWithoutReference;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/**
 * @author Steve Ebersole
 */
public class PersistenceUnitUtilImpl implements PersistenceUnitUtil, Serializable {
	private static final Logger log = Logger.getLogger( PersistenceUnitUtilImpl.class );

	private final SessionFactoryImplementor sessionFactory;
	private final transient PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	public PersistenceUnitUtilImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean isLoaded(Object entity, String attributeName) {
		// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
		log.debug( "PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead" );
		switch ( isLoadedWithoutReference( entity, attributeName, cache ) ) {
			case LOADED:
				return true;
			case NOT_LOADED:
				return false;
			default:
				return isLoadedWithReference( entity, attributeName, cache ) != NOT_LOADED;
		}
	}

	@Override
	public <E> boolean isLoaded(E entity, Attribute<? super E, ?> attribute) {
		return Hibernate.isPropertyInitialized( entity, attribute.getName() );
	}

	@Override
	public boolean isLoaded(Object entity) {
		// added log message to help with HHH-7454, if state == LoadState,NOT_LOADED, returning true or false is not accurate.
		log.debug( "PersistenceUnitUtil#isLoaded is not always accurate; consider using EntityManager#contains instead" );
		return getLoadState( entity ) != NOT_LOADED;
	}

	@Override
	public void load(Object entity, String attributeName) {
		Hibernate.initializeProperty( entity, attributeName );
	}

	@Override
	public <E> void load(E entity, Attribute<? super E, ?> attribute) {
		load( entity, attribute.getName() );
	}

	@Override
	public void load(Object entity) {
		Hibernate.initialize( entity );
	}

	@Override
	public boolean isInstance(Object entity, Class<?> entityClass) {
		return entityClass.isAssignableFrom( Hibernate.getClassLazy( entity ) );
	}

	@Override
	public <T> Class<? extends T> getClass(T entity) {
		return Hibernate.getClassLazy( entity );
	}

	@Override
	public Object getIdentifier(Object entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Passed entity cannot be null" );
		}

		final LazyInitializer lazyInitializer = extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			return lazyInitializer.getInternalIdentifier();
		}
		else if ( isManagedEntity( entity ) ) {
			final EntityEntry entityEntry = asManagedEntity( entity ).$$_hibernate_getEntityEntry();
			if ( entityEntry != null ) {
				return entityEntry.getId();
			}
			else {
				// HHH-11426 - best effort to deal with the case of detached entities
				log.debug( "jakarta.persistence.PersistenceUnitUtil.getIdentifier may not be able to read identifier of a detached entity" );
				return getIdentifierFromPersister( entity );
			}
		}
		else {
			log.debug(
					"jakarta.persistence.PersistenceUnitUtil.getIdentifier is only intended to work with enhanced entities " +
							"(although Hibernate also adapts this support to its proxies); " +
							"however the passed entity was not enhanced (nor a proxy).. may not be able to read identifier"
			);
			return getIdentifierFromPersister( entity );
		}
	}

	@Override
	public Object getVersion(Object entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Passed entity cannot be null" );
		}

		final LazyInitializer lazyInitializer = extractLazyInitializer( entity );
		if ( lazyInitializer != null ) {
			return getVersionFromPersister( lazyInitializer.getImplementation() );
		}
		else if ( isManagedEntity( entity ) ) {
			final EntityEntry entityEntry = asManagedEntity( entity ).$$_hibernate_getEntityEntry();
			if ( entityEntry != null ) {
				return entityEntry.getVersion();
			}
			else {
				return getVersionFromPersister( entity );
			}
		}
		else {
			return getVersionFromPersister( entity );
		}
	}

	private Object getIdentifierFromPersister(Object entity) {
		final Class<?> entityClass = Hibernate.getClass( entity );
		final EntityPersister persister;
		try {
			persister = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( entityClass );
			if ( persister == null ) {
				throw new IllegalArgumentException( entityClass.getName() + " is not an entity" );
			}
		}
		catch (MappingException ex) {
			throw new IllegalArgumentException( entityClass.getName() + " is not an entity", ex );
		}
		return persister.getIdentifier( entity );
	}

	private Object getVersionFromPersister(Object entity) {
		final Class<?> entityClass = Hibernate.getClass( entity );
		final EntityPersister persister;
		try {
			persister = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( entityClass );
			if ( persister == null ) {
				throw new IllegalArgumentException( entityClass.getName() + " is not an entity" );
			}
		}
		catch (MappingException ex) {
			throw new IllegalArgumentException( entityClass.getName() + " is not an entity", ex );
		}
		return persister.getVersion( entity );
	}

}
