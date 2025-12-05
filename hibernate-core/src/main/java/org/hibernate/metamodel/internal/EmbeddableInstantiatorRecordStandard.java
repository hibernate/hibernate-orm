/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

import static org.hibernate.internal.util.ReflectHelper.getConstructorOrNull;
import static org.hibernate.internal.util.ReflectHelper.getRecordComponentTypes;

/**
 * Support for instantiating embeddables as record representation
 */
public class EmbeddableInstantiatorRecordStandard extends AbstractPojoInstantiator implements EmbeddableInstantiator {

	protected final Constructor<?> constructor;

	public EmbeddableInstantiatorRecordStandard(Class<?> javaType) {
		super( javaType );
		constructor = getConstructorOrNull( javaType, getRecordComponentTypes( javaType ) );
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
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
