/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The standard Hibernate {@link PersisterFactory} implementation
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class PersisterFactoryImpl implements PersisterFactory, ServiceRegistryAwareService {
	/**
	 * The constructor signature for {@link EntityPersister} implementations
	 */
	public static final Class<?>[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			PersistentClass.class,
			EntityDataAccess.class,
			NaturalIdDataAccess.class,
			RuntimeModelCreationContext.class
	};

	/**
	 * The constructor signature for {@link CollectionPersister} implementations
	 */
	public static final Class<?>[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			Collection.class,
			CollectionDataAccess.class,
			RuntimeModelCreationContext.class
	};

	private PersisterClassResolver persisterClassResolver;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.persisterClassResolver = serviceRegistry.getService( PersisterClassResolver.class );
	}

	@Override
	public EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityDataAccess entityCacheAccessStrategy,
			NaturalIdDataAccess naturalIdCacheAccessStrategy,
			RuntimeModelCreationContext creationContext) {
		final Class<? extends EntityPersister> persisterClass = persisterClassResolver.getEntityPersisterClass( entityBinding );
		final Constructor<? extends EntityPersister> constructor = resolveEntityPersisterConstructor( persisterClass );
		try {
			return constructor.newInstance( entityBinding, entityCacheAccessStrategy, naturalIdCacheAccessStrategy, creationContext );
		}
		catch (MappingException e) {
			throw e;
		}
		catch (InvocationTargetException e) {
			final Throwable target = e.getTargetException();
			if ( target instanceof HibernateException hibernateException ) {
				throw hibernateException;
			}
			else {
				throw new MappingException(
						String.format(
								Locale.ROOT,
								"Could not instantiate persister %s (%s)",
								persisterClass.getName(),
								entityBinding.getEntityName()
						),
						target
				);
			}
		}
		catch (Exception e) {
			throw new MappingException(
					String.format(
							Locale.ROOT,
							"Could not instantiate persister %s (%s)",
							persisterClass.getName(),
							entityBinding.getEntityName()
					),
					e
			);
		}
	}

	private Constructor<? extends EntityPersister> resolveEntityPersisterConstructor(Class<? extends EntityPersister> persisterClass) {
		try {
			return persisterClass.getConstructor( ENTITY_PERSISTER_CONSTRUCTOR_ARGS );
		}
		catch (NoSuchMethodException noConstructorException) {
			throw new MappingException( "Could not find appropriate constructor for " + persisterClass.getName(), noConstructorException );
		}

	}

	@Override
	public CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			@Nullable CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) {
		final Class<? extends CollectionPersister> persisterClass = persisterClassResolver.getCollectionPersisterClass( collectionBinding );
		final Constructor<? extends CollectionPersister> constructor = resolveCollectionPersisterConstructor( persisterClass );
		try {
			return constructor.newInstance( collectionBinding, cacheAccessStrategy, creationContext );
		}
		catch (MappingException e) {
			throw e;
		}
		catch (InvocationTargetException e) {
			final Throwable target = e.getTargetException();
			if ( target instanceof HibernateException hibernateException ) {
				throw hibernateException;
			}
			else {
				throw new MappingException(
						String.format(
								"Could not instantiate collection persister implementor `%s` for collection-role `%s`",
								persisterClass.getName(),
								collectionBinding.getRole()
						),
						target
				);
			}
		}
		catch (Exception e) {
			throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), e );
		}
	}

	private Constructor<? extends CollectionPersister> resolveCollectionPersisterConstructor(Class<? extends CollectionPersister> persisterClass) {
		try {
			return persisterClass.getConstructor( COLLECTION_PERSISTER_CONSTRUCTOR_ARGS );
		}
		catch (NoSuchMethodException noConstructorException) {
			throw new MappingException( "Could not find appropriate constructor for " + persisterClass.getName(), noConstructorException );
		}
	}
}
