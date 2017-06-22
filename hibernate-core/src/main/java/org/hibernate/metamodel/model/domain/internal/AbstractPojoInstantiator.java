/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.bytecode.spi.ReflectionOptimizer.InstantiationOptimizer;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.model.domain.spi.Instantiator;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractPojoInstantiator implements Instantiator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractPojoInstantiator.class );

	private final Class mappedPojoClass;
	private final transient InstantiationOptimizer optimizer;

	private final boolean isAbstract;
	private final Constructor constructor;

	public AbstractPojoInstantiator(Class mappedPojoClass, InstantiationOptimizer optimizer) {
		this.mappedPojoClass = mappedPojoClass;
		this.optimizer = optimizer;

		this.isAbstract = ReflectHelper.isAbstractClass( mappedPojoClass );

		this.constructor = isAbstract || optimizer != null
				? null
				: resolveConstructor( mappedPojoClass );
	}

	private static Constructor resolveConstructor(Class mappedPojoClass) {
		try {
			return ReflectHelper.getDefaultConstructor( mappedPojoClass);
		}
		catch ( PropertyNotFoundException pnfe ) {
			LOG.noDefaultConstructor( mappedPojoClass.getName() );
		}

		return null;
	}

	protected Object instantiatePojo(SharedSessionContractImplementor session) {
		if ( isAbstract ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface: ", mappedPojoClass );
		}
		else if ( optimizer != null ) {
			return optimizer.newInstance();
		}
		else if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for entity: ", mappedPojoClass );
		}
		else {
			try {
				return constructor.newInstance( (Object[]) null );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity: ", mappedPojoClass, e );
			}
		}
	}

	@Override
	public boolean isInstance(Object object, SharedSessionContractImplementor session) {
		return mappedPojoClass.isInstance( object );
	}
}
