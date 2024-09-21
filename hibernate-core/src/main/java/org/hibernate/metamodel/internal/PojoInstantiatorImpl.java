/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;

import org.hibernate.PropertyNotFoundException;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class PojoInstantiatorImpl<J> extends AbstractPojoInstantiator {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PojoInstantiatorImpl.class );

	private final Constructor<?> constructor;

	public PojoInstantiatorImpl(JavaType javaType) {
		super( javaType.getJavaTypeClass() );

		this.constructor = isAbstract()
				? null
				: resolveConstructor( getMappedPojoClass() );
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

	protected Object applyInterception(Object entity) {
		return entity;
	}

}
