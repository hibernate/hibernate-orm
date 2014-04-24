/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.tuple;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.EntityIdentifier;
import org.hibernate.service.ServiceRegistry;

import org.jboss.logging.Logger;

/**
 * Defines a POJO-based instantiator for use from the tuplizers.
 */
public class PojoInstantiator implements Instantiator, Serializable {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, PojoInstantiator.class.getName());

	private transient Constructor constructor;

	private final Class mappedClass;
	private final transient ReflectionOptimizer.InstantiationOptimizer optimizer;
	private final boolean embeddedIdentifier;
	private final Class proxyInterface;
	private final boolean isAbstract;

	public PojoInstantiator(
			ServiceRegistry serviceRegistry,
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper,
			ReflectionOptimizer.InstantiationOptimizer optimizer) {
		final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
		if ( isIdentifierMapper ) {
			final EntityIdentifier entityIdentifier =
					embeddableBinding.seekEntityBinding().getHierarchyDetails().getEntityIdentifier();
			final EntityIdentifier.NonAggregatedCompositeIdentifierBinding idBinding =
					(EntityIdentifier.NonAggregatedCompositeIdentifierBinding) entityIdentifier.getEntityIdentifierBinding();
			this.mappedClass = cls.classForName(
					idBinding.getIdClassMetadata().getIdClassType().getName().toString()
			);
		}
		else {
			this.mappedClass = cls.classForName(
					embeddableBinding.getAttributeContainer().getDescriptor().getName().toString()
			);
		}
		this.isAbstract = ReflectHelper.isAbstractClass( mappedClass );
		this.optimizer = optimizer;

		this.proxyInterface = null;
		this.embeddedIdentifier = false;

		try {
			constructor = ReflectHelper.getDefaultConstructor(mappedClass);
		}
		catch ( PropertyNotFoundException pnfe ) {
			LOG.noDefaultConstructor(mappedClass.getName());
			constructor = null;
		}
	}

	public PojoInstantiator(
			ServiceRegistry serviceRegistry,
			EntityBinding entityBinding,
			ReflectionOptimizer.InstantiationOptimizer optimizer) {
		final ClassLoaderService cls = serviceRegistry.getService( ClassLoaderService.class );
		this.mappedClass = cls.classForName( entityBinding.getEntity().getDescriptor().getName().toString() );
		this.isAbstract = ReflectHelper.isAbstractClass( mappedClass );
		if ( entityBinding.getProxyInterfaceType() == null ) {
			this.proxyInterface = null;
		}
		else {
			this.proxyInterface = cls.classForName(
					entityBinding.getProxyInterfaceType().getName().toString()
			);
		}
		this.embeddedIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier().getNature() == EntityIdentifierNature.NON_AGGREGATED_COMPOSITE;
		this.optimizer = optimizer;

		try {
			constructor = ReflectHelper.getDefaultConstructor( mappedClass );
		}
		catch ( PropertyNotFoundException pnfe ) {
			LOG.noDefaultConstructor(mappedClass.getName());
			constructor = null;
		}
	}

	private void readObject(java.io.ObjectInputStream stream)
	throws ClassNotFoundException, IOException {
		stream.defaultReadObject();
		constructor = ReflectHelper.getDefaultConstructor( mappedClass );
	}

	public Object instantiate() {
		if ( isAbstract ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface: ", mappedClass );
		}
		else if ( optimizer != null ) {
			return optimizer.newInstance();
		}
		else if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for entity: ", mappedClass );
		}
		else {
			try {
				return constructor.newInstance( (Object[]) null );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity: ", mappedClass, e );
			}
		}
	}

	public Object instantiate(Serializable id) {
		final boolean useEmbeddedIdentifierInstanceAsEntity = embeddedIdentifier &&
				id != null &&
				id.getClass().equals(mappedClass);
		return useEmbeddedIdentifierInstanceAsEntity ? id : instantiate();
	}

	public boolean isInstance(Object object) {
		return mappedClass.isInstance(object) ||
				( proxyInterface!=null && proxyInterface.isInstance(object) ); //this one needed only for guessEntityMode()
	}
}
