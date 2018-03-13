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
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The standard Hibernate {@link PersisterFactory} implementation
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class PersisterFactoryImpl implements PersisterFactory, ServiceRegistryAwareService {

	// todo : carry the notion of the creational parameters (parameter object) over to the persister constructors?

	/**
	 * The constructor signature for {@link EntityPersister} implementations
	 */
	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			PersistentClass.class,
			EntityDataAccess.class,
			NaturalIdDataAccess.class,
			PersisterCreationContext.class
	};

	/**
	 * The constructor signature for {@link CollectionPersister} implementations
	 */
	public static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			Collection.class,
			CollectionDataAccess.class,
			PersisterCreationContext.class
	};

	private ServiceRegistryImplementor serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public EntityPersister createEntityPersister(
			PersistentClass entityBinding,
			EntityDataAccess entityCacheAccessStrategy,
			NaturalIdDataAccess naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		// If the metadata for the entity specified an explicit persister class, use it...
		Class<? extends EntityPersister> persisterClass = entityBinding.getEntityPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getEntityPersisterClass( entityBinding );
		}

		return createEntityPersister(
				persisterClass,
				entityBinding,
				entityCacheAccessStrategy,
				naturalIdCacheAccessStrategy,
				creationContext
		);
	}

	@SuppressWarnings( {"unchecked"})
	private EntityPersister createEntityPersister(
			Class<? extends EntityPersister> persisterClass,
			PersistentClass entityBinding,
			EntityDataAccess entityCacheAccessStrategy,
			NaturalIdDataAccess naturalIdCacheAccessStrategy,
			PersisterCreationContext creationContext) {
		try {
			final Constructor<? extends EntityPersister> constructor = persisterClass.getConstructor( ENTITY_PERSISTER_CONSTRUCTOR_ARGS );
			try {
				return constructor.newInstance(
						entityBinding,
						entityCacheAccessStrategy,
						naturalIdCacheAccessStrategy,
						creationContext
				);
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
					throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public CollectionPersister createCollectionPersister(
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		// If the metadata for the collection specified an explicit persister class, use it
		Class<? extends CollectionPersister> persisterClass = collectionBinding.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class )
					.getCollectionPersisterClass( collectionBinding );
		}
		return createCollectionPersister( persisterClass, collectionBinding, cacheAccessStrategy, creationContext );
	}

	@SuppressWarnings( {"unchecked"})
	private CollectionPersister createCollectionPersister(
			Class<? extends CollectionPersister> persisterClass,
			Collection collectionBinding,
			CollectionDataAccess cacheAccessStrategy,
			PersisterCreationContext creationContext) {
		try {
			Constructor<? extends CollectionPersister> constructor = persisterClass.getConstructor( COLLECTION_PERSISTER_CONSTRUCTOR_ARGS );
			try {
				return constructor.newInstance(
						collectionBinding,
						cacheAccessStrategy,
						creationContext
				);
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
					throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate collection persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}
}
