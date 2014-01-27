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
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.spi.PersisterClassResolver;
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

	/**
	 * The constructor signature for {@link EntityPersister} implementations
	 *
	 * @todo make EntityPersister *not* depend on {@link SessionFactoryImplementor} if possible.
	 */
	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = new Class[] {
			PersistentClass.class,
			EntityRegionAccessStrategy.class,
			NaturalIdRegionAccessStrategy.class,
			SessionFactoryImplementor.class,
			Mapping.class
	};

	/**
	 * The constructor signature for {@link EntityPersister} implementations using
	 * an {@link EntityBinding}.
	 *
	 * @todo make EntityPersister *not* depend on {@link SessionFactoryImplementor} if possible.
	 * @todo change ENTITY_PERSISTER_CONSTRUCTOR_ARGS_NEW to ENTITY_PERSISTER_CONSTRUCTOR_ARGS
	 * when new metamodel is integrated
	 */
	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS_NEW = new Class[] {
			EntityBinding.class,
			EntityRegionAccessStrategy.class,
			NaturalIdRegionAccessStrategy.class,
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

	/**
	 * The constructor signature for {@link CollectionPersister} implementations using
	 * a {@link org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding}
	 *
	 * @todo still need to make collection persisters EntityMode-aware
	 * @todo make EntityPersister *not* depend on {@link SessionFactoryImplementor} if possible.
	 * @todo change COLLECTION_PERSISTER_CONSTRUCTOR_ARGS_NEW to COLLECTION_PERSISTER_CONSTRUCTOR_ARGS
	 * when new metamodel is integrated
	 */
	private static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS_NEW = new Class[] {
			AbstractPluralAttributeBinding.class,
			CollectionRegionAccessStrategy.class,
			MetadataImplementor.class,
			SessionFactoryImplementor.class
	};

	private ServiceRegistryImplementor serviceRegistry;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public EntityPersister createEntityPersister(
			PersistentClass metadata,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) {
		Class<? extends EntityPersister> persisterClass = metadata.getEntityPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getEntityPersisterClass( metadata );
		}
		return create( persisterClass, ENTITY_PERSISTER_CONSTRUCTOR_ARGS, metadata, cacheAccessStrategy, naturalIdAccessStrategy, factory, cfg );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public EntityPersister createEntityPersister(
			EntityBinding metadata,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) {
		Class<? extends EntityPersister> persisterClass = metadata.getCustomEntityPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getEntityPersisterClass( metadata );
		}
		return create( persisterClass, ENTITY_PERSISTER_CONSTRUCTOR_ARGS_NEW, metadata, cacheAccessStrategy, naturalIdAccessStrategy, factory, cfg );
	}

	// TODO: change metadata arg type to EntityBinding when new metadata is integrated
	private static EntityPersister create(
			Class<? extends EntityPersister> persisterClass,
			Class[] persisterConstructorArgs,
			Object metadata,
			EntityRegionAccessStrategy cacheAccessStrategy,
			NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			SessionFactoryImplementor factory,
			Mapping cfg) throws HibernateException {
		try {
			Constructor<? extends EntityPersister> constructor = persisterClass.getConstructor( persisterConstructorArgs );
			try {
				return constructor.newInstance( metadata, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory, cfg );
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
			Collection collectionMetadata,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException {
		Class<? extends CollectionPersister> persisterClass = collectionMetadata.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getCollectionPersisterClass( collectionMetadata );
		}

		return create( persisterClass, COLLECTION_PERSISTER_CONSTRUCTOR_ARGS, cfg, collectionMetadata, cacheAccessStrategy, factory );
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public CollectionPersister createCollectionPersister(
			MetadataImplementor metadata,
			PluralAttributeBinding collectionMetadata,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException {
		Class<? extends CollectionPersister> persisterClass = collectionMetadata.getExplicitPersisterClass();
		if ( persisterClass == null ) {
			persisterClass = serviceRegistry.getService( PersisterClassResolver.class ).getCollectionPersisterClass( collectionMetadata );
		}

		return create( persisterClass, COLLECTION_PERSISTER_CONSTRUCTOR_ARGS_NEW,  metadata, collectionMetadata, cacheAccessStrategy, factory );
	}

	// TODO: change collectionMetadata arg type to AbstractPluralAttributeBinding when new metadata is integrated
	// TODO: change metadata arg type to MetadataImplementor when new metadata is integrated
	private static CollectionPersister create(
			Class<? extends CollectionPersister> persisterClass,
			Class[] persisterConstructorArgs,
			Object cfg,
			Object collectionMetadata,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			SessionFactoryImplementor factory) throws HibernateException {
		try {
			Constructor<? extends CollectionPersister> constructor = persisterClass.getConstructor( persisterConstructorArgs );
			try {
				return constructor.newInstance( collectionMetadata, cacheAccessStrategy, cfg, factory );
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
