/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
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
		// If the metadata for the entity specified an explicit persister class, use it...
		Class<? extends EntityPersister> persisterClass = entityBinding.getEntityPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = persisterClassResolver.getEntityPersisterClass( entityBinding );
		}

		return createEntityPersister(
				persisterClass,
				entityBinding,
				entityCacheAccessStrategy,
				naturalIdCacheAccessStrategy,
				creationContext
		);
	}

	private EntityPersister createEntityPersister(
			Class<? extends EntityPersister> persisterClass,
			PersistentClass entityBinding,
			EntityDataAccess entityCacheAccessStrategy,
			NaturalIdDataAccess naturalIdCacheAccessStrategy,
			RuntimeModelCreationContext creationContext) {
		final Constructor<? extends EntityPersister> constructor = resolveEntityPersisterConstructor( persisterClass );
		try {
			return constructor.newInstance( entityBinding, entityCacheAccessStrategy, naturalIdCacheAccessStrategy, creationContext );
		}
		catch (MappingException e) {
			throw e;
		}
		catch (InvocationTargetException e) {
			final Throwable target = e.getTargetException();
			if ( target instanceof HibernateException ) {
				throw (HibernateException) target;
			}
			else {
				throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), target );
			}
		}
		catch (Exception e) {
			throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), e );
		}
	}

	private Constructor<? extends EntityPersister> resolveEntityPersisterConstructor(Class<? extends EntityPersister> persisterClass) {
		try {
			return persisterClass.getConstructor( ENTITY_PERSISTER_CONSTRUCTOR_ARGS );
		}
		catch (NoSuchMethodException noConstructorException) {
			// we could not find the constructor...
			//
			// until we drop support for the legacy constructor signature, see if they define a
			// constructor using that signature and use it if so
			try {
				final Constructor<? extends EntityPersister> constructor = persisterClass.getConstructor( LEGACY_ENTITY_PERSISTER_CONSTRUCTOR_ARGS );
				// they do use the legacy signature...

				// warn them
				DeprecationLogger.DEPRECATION_LOGGER.debugf(
						"EntityPersister implementation defined constructor using legacy signature using `%s`; use `%s` instead",
						PersisterCreationContext.class.getName(),
						RuntimeModelCreationContext.class.getName()
				);

				// but use it
				return constructor;
			}
			catch (NoSuchMethodException noLegacyConstructorException) {
				// fall through to below
			}

			throw new MappingException( "Could not find appropriate constructor for " + persisterClass.getName(), noConstructorException );
		}

	}

	@Override
	public CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			@Nullable CollectionDataAccess cacheAccessStrategy,
			RuntimeModelCreationContext creationContext) {
		// If the metadata for the collection specified an explicit persister class, use it
		Class<? extends CollectionPersister> persisterClass = collectionBinding.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = persisterClassResolver.getCollectionPersisterClass( collectionBinding );
		}

		return createCollectionPersister( persisterClass, collectionBinding, cacheAccessStrategy, creationContext );
	}

	private CollectionPersister createCollectionPersister(
			Class<? extends CollectionPersister> persisterClass,
			Collection collectionBinding,
			@Nullable CollectionDataAccess cacheAccessStrategy,
			@SuppressWarnings("deprecation") PersisterCreationContext creationContext) {
		final Constructor<? extends CollectionPersister> constructor = resolveCollectionPersisterConstructor( persisterClass );
		try {
			return constructor.newInstance( collectionBinding, cacheAccessStrategy, creationContext );
		}
		catch (MappingException e) {
			throw e;
		}
		catch (InvocationTargetException e) {
			Throwable target = e.getTargetException();
			if ( target instanceof HibernateException ) {
				throw (HibernateException) target;
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
			// we could not find the constructor...
			//
			// until we drop support for the legacy constructor signature, see if they define a
			// constructor using that signature and use it if so
			try {
				final Constructor<? extends CollectionPersister> constructor = persisterClass.getConstructor( LEGACY_COLLECTION_PERSISTER_CONSTRUCTOR_ARGS );
				// they do use the legacy signature...

				// warn them
				DeprecationLogger.DEPRECATION_LOGGER.debugf(
						"CollectionPersister implementation defined constructor using legacy signature using `%s`; use `%s` instead",
						PersisterCreationContext.class.getName(),
						RuntimeModelCreationContext.class.getName()
				);

				// but use it
				return constructor;
			}
			catch (NoSuchMethodException noLegacyConstructorException) {
				// fall through to below
			}

			throw new MappingException( "Could not find appropriate constructor for " + persisterClass.getName(), noConstructorException );
		}
	}


	/**
	 * The legacy constructor signature for {@link EntityPersister} implementations
	 *
	 * @deprecated use {@link #ENTITY_PERSISTER_CONSTRUCTOR_ARGS} instead
	 */
	@Deprecated(since = "6.0")
	private static final Class<?>[] LEGACY_ENTITY_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			PersistentClass.class,
			EntityDataAccess.class,
			NaturalIdDataAccess.class,
			PersisterCreationContext.class
	};

	@Override
	public EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityDataAccess entityCacheAccessStrategy,
			NaturalIdDataAccess naturalIdCacheAccessStrategy,
			@SuppressWarnings("deprecation") PersisterCreationContext creationContext) {
		DeprecationLogger.DEPRECATION_LOGGER.debugf(
				"Encountered use of deprecated `PersisterFactory#createEntityPersister` form accepting `%s`; use form accepting `%s` instead",
				PersisterCreationContext.class.getName(),
				RuntimeModelCreationContext.class.getName()
		);

		return createEntityPersister(
				entityBinding,
				entityCacheAccessStrategy,
				naturalIdCacheAccessStrategy,
				(RuntimeModelCreationContext) creationContext
		);
	}

	/**
	 * The constructor signature for {@link CollectionPersister} implementations
	 *
	 * @deprecated use {@link #COLLECTION_PERSISTER_CONSTRUCTOR_ARGS} instead
	 */
	@Deprecated(since = "6.0")
	private static final Class<?>[] LEGACY_COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			Collection.class,
			CollectionDataAccess.class,
			PersisterCreationContext.class
	};


	@Override
	public CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			@SuppressWarnings("deprecation") PersisterCreationContext creationContext) throws HibernateException {
		DeprecationLogger.DEPRECATION_LOGGER.debugf(
				"Encountered use of deprecated `PersisterFactory#createCollectionPersister` form accepting `%s`; use form accepting `%s` instead",
				PersisterCreationContext.class.getName(),
				RuntimeModelCreationContext.class.getName()
		);

		return createCollectionPersister( collectionBinding, cacheAccessStrategy, (RuntimeModelCreationContext) creationContext );
	}
}
