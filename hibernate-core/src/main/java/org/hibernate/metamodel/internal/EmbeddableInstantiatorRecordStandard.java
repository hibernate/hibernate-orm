/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * Support for instantiating embeddables as record representation
 */
public class EmbeddableInstantiatorRecordStandard extends AbstractPojoInstantiator implements EmbeddableInstantiator {

	protected final Constructor<?> constructor;

	public EmbeddableInstantiatorRecordStandard(Class<?> javaType) {
		super( javaType );

		final Class<?>[] componentTypes = ReflectHelper.getRecordComponentTypes( javaType );
		this.constructor = ReflectHelper.getConstructorOrNull( javaType, componentTypes );
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess, SessionFactoryImplementor sessionFactory) {
		if ( constructor == null ) {
			throw new InstantiationException( "Unable to locate constructor for embeddable", getMappedPojoClass() );
		}

		try {
			return constructor.newInstance( valuesAccess.getValues() );
		}
		catch ( Exception e ) {
			throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
		}
	}
}
