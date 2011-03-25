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

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
import org.hibernate.persister.spi.PersisterFactory;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryAwareService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The standard Hibernate {@link PersisterFactory} implementation
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class PersisterFactoryImpl implements PersisterFactory, ServiceRegistryAwareService {

	/**
	 * The constructor signature for {@link EntityPersister} implementations
	 *
	 * @todo make EntityPersister *not* depend on {@link SessionFactoryImplementor} if possible.
	 */
	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			PersistentClass.class,
			EntityRegionAccessStrategy.class,
			SessionFactoryImplementor.class,
			Mapping.class
	};

	/**
	 * The constructor signature for {@link CollectionPersister} implementations
	 *
	 * @todo still need to make collection persisters EntityMode-aware
	 * @todo make EntityPersister *not* depend on {@link SessionFactoryImplementor} if possible.
	 */
	private static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			Collection.class,
			CollectionRegionAccessStrategy.class,
			Configuration.class,
			SessionFactoryImplementor.class
	};

	private ServiceRegistry serviceRegistry;

	@Override
	public void injectServices(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public EntityPersister createEntityPersister(
			PersistentClass metadata,
			EntityRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) {
		Class<? extends EntityPersister> persisterClass = metadata.getEntityPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getEntityPersisterClass( metadata );
		}
		return create( persisterClass, metadata, cacheAccessStrategy, factory, cfg );
	}

	private static EntityPersister create(
			Class<? extends EntityPersister> persisterClass,
			PersistentClass metadata,
			EntityRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) throws HibernateException {
		try {
			Constructor<? extends EntityPersister> constructor = persisterClass.getConstructor( ENTITY_PERSISTER_CONSTRUCTOR_ARGS );
			try {
				return constructor.newInstance( metadata, cacheAccessStrategy, factory, cfg );
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
			Configuration cfg,
			Collection metadata,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException {
		Class<? extends CollectionPersister> persisterClass = metadata.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getCollectionPersisterClass( metadata );
		}

		return create( persisterClass, cfg, metadata, cacheAccessStrategy, factory );
	}

	private static CollectionPersister create(
			Class<? extends CollectionPersister> persisterClass,
			Configuration cfg,
			Collection metadata,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException {
		try {
			Constructor<? extends CollectionPersister> constructor = persisterClass.getConstructor( COLLECTION_PERSISTER_CONSTRUCTOR_ARGS );
			try {
				return constructor.newInstance( metadata, cacheAccessStrategy, cfg, factory );
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
