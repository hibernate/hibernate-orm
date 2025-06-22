/*
 * SPDX-License-Identifier: Apache-2.0
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

	private final SessionFactoryImplementor sessionFactory;
	private final transient PersistenceUtilHelper.MetadataCache cache = new PersistenceUtilHelper.MetadataCache();

	public PersistenceUnitUtilImpl(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public boolean isLoaded(Object entity, String attributeName) {
		return switch ( isLoadedWithoutReference( entity, attributeName, cache ) ) {
			case LOADED -> true;
			case NOT_LOADED -> false;
			default -> isLoadedWithReference( entity, attributeName, cache ) != NOT_LOADED;
		};
	}

	@Override
	public <E> boolean isLoaded(E entity, Attribute<? super E, ?> attribute) {
		return Hibernate.isPropertyInitialized( entity, attribute.getName() );
	}

	@Override
	public boolean isLoaded(Object entity) {
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
			throw new IllegalArgumentException( "Entity may not be null" );
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
				return getIdentifierFromPersister( entity );
			}
		}
		else {
			return getIdentifierFromPersister( entity );
		}
	}

	@Override
	public Object getVersion(Object entity) {
		if ( entity == null ) {
			throw new IllegalArgumentException( "Entity may not be null" );
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
			persister =
					sessionFactory.getMappingMetamodel()
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
			persister =
					sessionFactory.getMappingMetamodel()
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
