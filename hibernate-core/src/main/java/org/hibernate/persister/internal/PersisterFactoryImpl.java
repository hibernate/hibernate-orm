/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.persister.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
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
			EntityRegionAccessStrategy.class,
			NaturalIdRegionAccessStrategy.class,
			PersisterCreationContext.class
	};

	/**
	 * The constructor signature for {@link CollectionPersister} implementations
	 */
	public static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			Collection.class,
			CollectionRegionAccessStrategy.class,
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
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
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
			EntityRegionAccessStrategy entityCacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdCacheAccessStrategy,
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
			CollectionRegionAccessStrategy cacheAccessStrategy,
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
			CollectionRegionAccessStrategy cacheAccessStrategy,
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
