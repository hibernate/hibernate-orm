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

import org.jboss.logging.Logger;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.binding.EntityBinding;

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

	public PojoInstantiator(Component component, ReflectionOptimizer.InstantiationOptimizer optimizer) {
		this.mappedClass = component.getComponentClass();
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

	public PojoInstantiator(PersistentClass persistentClass, ReflectionOptimizer.InstantiationOptimizer optimizer) {
		this.mappedClass = persistentClass.getMappedClass();
		this.isAbstract = ReflectHelper.isAbstractClass( mappedClass );
		this.proxyInterface = persistentClass.getProxyInterface();
		this.embeddedIdentifier = persistentClass.hasEmbeddedIdentifier();
		this.optimizer = optimizer;

		try {
			constructor = ReflectHelper.getDefaultConstructor( mappedClass );
		}
		catch ( PropertyNotFoundException pnfe ) {
			LOG.noDefaultConstructor(mappedClass.getName());
			constructor = null;
		}
	}

	public PojoInstantiator(EntityBinding entityBinding, ReflectionOptimizer.InstantiationOptimizer optimizer) {
		this.mappedClass = entityBinding.getEntity().getClassReference();
		this.isAbstract = ReflectHelper.isAbstractClass( mappedClass );
		this.proxyInterface = entityBinding.getProxyInterfaceType().getValue();
		this.embeddedIdentifier = entityBinding.getHierarchyDetails().getEntityIdentifier().isEmbedded();
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