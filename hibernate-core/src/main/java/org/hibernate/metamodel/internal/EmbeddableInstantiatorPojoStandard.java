/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Support for instantiating embeddables as POJO representation
 *
 * @author Steve Ebersole
 */
public class EmbeddableInstantiatorPojoStandard extends AbstractPojoInstantiator implements EmbeddableInstantiator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PojoInstantiatorImpl.class );

	private final Constructor<?> constructor;

	public EmbeddableInstantiatorPojoStandard(
			@SuppressWarnings("unused") Component bootDescriptor,
			JavaType<?> javaTypeDescriptor) {
		super( javaTypeDescriptor.getJavaTypeClass() );

		constructor = resolveConstructor( javaTypeDescriptor.getJavaTypeClass() );

		// todo (6.0) : add support for constructor value injection
	}

	protected static Constructor<?> resolveConstructor(Class<?> mappedPojoClass) {
		try {
			return ReflectHelper.getDefaultConstructor( mappedPojoClass);
		}
		catch ( PropertyNotFoundException e ) {
			LOG.noDefaultConstructor( mappedPojoClass.getName() );
		}

		return null;
	}

	@Override
	public Object instantiate(Supplier<Object[]> valuesAccess, SessionFactoryImplementor sessionFactory) {
		if ( isAbstract() ) {
			throw new InstantiationException( "Cannot instantiate abstract class or interface: ", getMappedPojoClass() );
		}
		else if ( constructor == null ) {
			throw new InstantiationException( "No default constructor for embeddable: ", getMappedPojoClass() );
		}
		else {
			try {
				return constructor.newInstance( (Object[]) null );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity: ", getMappedPojoClass(), e );
			}
		}
	}
}
