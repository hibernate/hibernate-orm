/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.creation.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.model.domain.spi.EmbeddedValueMappingImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorClassResolver;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelDescriptorFactory;
import org.hibernate.metamodel.model.domain.spi.EmbeddedContainer;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.MappedSuperclassTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * The standard ORM implementation of the {@link RuntimeModelDescriptorFactory} contract
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public final class RuntimeModelDescriptorFactoryImpl
		implements RuntimeModelDescriptorFactory, ServiceRegistryAwareService {
	private RuntimeModelDescriptorClassResolver descriptorClassResolver;

	@Override
	public void injectServices(ServiceRegistryImplementor serviceRegistry) {
		this.descriptorClassResolver = serviceRegistry.getService( RuntimeModelDescriptorClassResolver.class );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <J> EntityTypeDescriptor<J> createEntityDescriptor(
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		return instantiateEntityDescriptor(
				bootMapping,
				superTypeDescriptor,
				creationContext
		);
	}

	@SuppressWarnings("unchecked")
	private EntityTypeDescriptor instantiateEntityDescriptor(
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		// If the metadata for the entity specified an explicit persister class, use it...
		Class<? extends EntityTypeDescriptor> entityDescriptorClass = bootMapping.getRuntimeEntityDescriptorClass();
		if ( entityDescriptorClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			entityDescriptorClass = descriptorClassResolver.getEntityDescriptorClass( bootMapping );
		}

		return instantiateEntityDescriptor(
				entityDescriptorClass,
				bootMapping,
				superTypeDescriptor,
				creationContext
		);
	}

	@SuppressWarnings( {"unchecked"})
	private EntityTypeDescriptor instantiateEntityDescriptor(
			Class<? extends EntityTypeDescriptor> persisterClass,
			EntityMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {

		try {
			final Constructor<? extends EntityTypeDescriptor> constructor = persisterClass.getConstructor( EntityTypeDescriptor.STANDARD_CONSTRUCTOR_SIG );
			try {
				return constructor.newInstance(
						bootMapping,
						superTypeDescriptor,
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
	@SuppressWarnings("unchecked")
	public <J> MappedSuperclassTypeDescriptor<J> createMappedSuperclassDescriptor(
			MappedSuperclassMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		return instantiateMappedSuperclassDescriptor(
				bootMapping,
				superTypeDescriptor,
				creationContext
		);
	}

	@SuppressWarnings("unchecked")
	private MappedSuperclassTypeDescriptor instantiateMappedSuperclassDescriptor(
			MappedSuperclassMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		// currently we do not allow user to explicitly name a descriptor class to use on the mapping.
		// so just look to the resolver
		final Class<? extends MappedSuperclassTypeDescriptor> runtimeDescriptorClass =
				descriptorClassResolver.getMappedSuperclassDescriptorClass( bootMapping );

		return instantiateMappedSuperclassDescriptor(
				runtimeDescriptorClass,
				bootMapping,
				superTypeDescriptor,
				creationContext
		);
	}

	@SuppressWarnings( {"unchecked"})
	private MappedSuperclassTypeDescriptor instantiateMappedSuperclassDescriptor(
			Class<? extends MappedSuperclassTypeDescriptor> descriptorClass,
			MappedSuperclassMapping bootMapping,
			IdentifiableTypeDescriptor superTypeDescriptor,
			RuntimeModelCreationContext creationContext) {
		try {
			final Constructor<? extends MappedSuperclassTypeDescriptor> constructor = descriptorClass.getConstructor( MappedSuperclassTypeDescriptor.STANDARD_CONSTRUCTOR_SIG );
			try {
				final MappedSuperclassTypeDescriptor descriptor = constructor.newInstance(
						bootMapping,
						superTypeDescriptor,
						creationContext
				);
				creationContext.registerMappedSuperclassDescriptor( descriptor, bootMapping );
				return descriptor;
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
					throw new MappingException( "Could not instantiate persister " + descriptorClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate persister " + descriptorClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + descriptorClass.getName(), e );
		}
	}


	@Override
	@SuppressWarnings( {"unchecked"})
	public <O,C,E> PersistentCollectionDescriptor<O,C,E>  createPersistentCollectionDescriptor(
			Property pluralProperty,
			ManagedTypeDescriptor<O> runtimeManagedType,
			RuntimeModelCreationContext creationContext) throws HibernateException {
		final Collection collectionMapping = (Collection) pluralProperty.getValue();

		// If the metadata for the collection specified an explicit persister class, use it
		Class<? extends PersistentCollectionDescriptor> persisterClass = collectionMapping.getCollectionPersisterClass();
		if ( persisterClass == null ) {
			// Otherwise, use the persister class indicated by the PersisterClassResolver service
			persisterClass = descriptorClassResolver.getCollectionDescriptorClass( collectionMapping );
		}
		return createCollectionDescriptor( persisterClass, pluralProperty, collectionMapping, runtimeManagedType, creationContext );
	}

	@SuppressWarnings( {"unchecked"})
	private PersistentCollectionDescriptor createCollectionDescriptor(
			Class<? extends PersistentCollectionDescriptor> persisterClass,
			Property pluralProperty,
			Collection collectionBinding,
			ManagedTypeDescriptor runtimeManagedType,
			RuntimeModelCreationContext creationContext) {
		try {
			Constructor<? extends PersistentCollectionDescriptor> constructor = persisterClass.getConstructor( PersistentCollectionDescriptor.CONSTRUCTOR_SIGNATURE );
			try {
				final PersistentCollectionDescriptor descriptor = constructor.newInstance(
						pluralProperty,
						runtimeManagedType,
						creationContext
				);
				creationContext.registerCollectionDescriptor( descriptor, collectionBinding );
				return descriptor;
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

	@Override
	@SuppressWarnings( {"unchecked"})
	public EmbeddedTypeDescriptor createEmbeddedTypeDescriptor(
			EmbeddedValueMappingImplementor bootValueMapping,
			EmbeddedContainer source,
			EmbeddedTypeDescriptor superTypeDescriptor,
			String localName,
			SingularPersistentAttribute.Disposition disposition,
			RuntimeModelCreationContext creationContext) {
		final Class<? extends EmbeddedTypeDescriptor> persisterClass = descriptorClassResolver.getEmbeddedTypeDescriptorClass( bootValueMapping );

		try {
			Constructor<? extends EmbeddedTypeDescriptor> constructor = persisterClass.getConstructor( EmbeddedTypeDescriptor.STANDARD_CTOR_SIGNATURE );
			try {
				final EmbeddedTypeDescriptor descriptor = constructor.newInstance(
						bootValueMapping,
						source,
						superTypeDescriptor,
						localName,
						disposition,
						creationContext
				);
				creationContext.registerEmbeddableDescriptor( descriptor, bootValueMapping );
				return descriptor;
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
					throw new MappingException( "Could not instantiate embedded persister " + persisterClass.getName(), target );
				}
			}
			catch (Exception e) {
				throw new MappingException( "Could not instantiate embedded persister " + persisterClass.getName(), e );
			}
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MappingException( "Could not get constructor for " + persisterClass.getName(), e );
		}
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Deprecations

	/**
	 * @deprecated Use {@link EntityTypeDescriptor#STANDARD_CONSTRUCTOR_SIG} instead.
	 */
	@Deprecated
	public static final Class[] ENTITY_PERSISTER_CONSTRUCTOR_ARGS = EntityTypeDescriptor.STANDARD_CONSTRUCTOR_SIG;

	/**
	 * @deprecated Use {@link PersistentCollectionDescriptor#CONSTRUCTOR_SIGNATURE} instead
	 */
	@Deprecated
	public static final Class[] COLLECTION_PERSISTER_CONSTRUCTOR_ARGS = PersistentCollectionDescriptor.CONSTRUCTOR_SIGNATURE;
}
