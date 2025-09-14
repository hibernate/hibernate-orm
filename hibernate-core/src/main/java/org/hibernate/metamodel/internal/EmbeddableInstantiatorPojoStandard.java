/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

import org.hibernate.InstantiationException;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.ValueAccess;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;
import static org.hibernate.internal.util.ReflectHelper.getDefaultConstructor;

/**
 * Support for instantiating embeddables as POJO representation
 */
public class EmbeddableInstantiatorPojoStandard extends AbstractPojoInstantiator implements StandardEmbeddableInstantiator {

	private final Supplier<EmbeddableMappingType> embeddableMappingAccess;
	private final Constructor<?> constructor;

	public EmbeddableInstantiatorPojoStandard(Class<?> embeddableClass, Supplier<EmbeddableMappingType> embeddableMappingAccess) {
		super( embeddableClass );
		this.embeddableMappingAccess = embeddableMappingAccess;
		this.constructor = resolveConstructor( embeddableClass );
	}

	protected static Constructor<?> resolveConstructor(Class<?> mappedPojoClass) {
		try {
			return getDefaultConstructor( mappedPojoClass );
		}
		catch ( PropertyNotFoundException e ) {
			CORE_LOGGER.noDefaultConstructor( mappedPojoClass.getName() );
			return null;
		}
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		if ( isAbstract() ) {
			throw new InstantiationException(
					"Cannot instantiate abstract class or interface", getMappedPojoClass()
			);
		}

		if ( constructor == null ) {
			throw new InstantiationException( "Unable to locate constructor for embeddable", getMappedPojoClass() );
		}

		try {
			final Object[] values = valuesAccess == null ? null : valuesAccess.getValues();
			final Object instance = constructor.newInstance();
			if ( values != null ) {
				// At this point, createEmptyCompositesEnabled is always true.
				// We can only set the property values on the compositeInstance though if there is at least one non null value.
				// If the values are all null, we would normally not create a composite instance at all because no values exist.
				// Setting all properties to null could cause IllegalArgumentExceptions though when the component has primitive properties.
				// To avoid this exception and align with what Hibernate 5 did, we skip setting properties if all values are null.
				// A possible alternative could be to initialize the resolved values for primitive fields to their default value,
				// but that might cause unexpected outcomes for Hibernate 5 users that use createEmptyCompositesEnabled when updating.
				// You can see the need for this by running EmptyCompositeEquivalentToNullTest
				embeddableMappingAccess.get().setValues( instance, values );
			}

			return instance;
		}
		catch ( Exception e ) {
			throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
		}
	}
}
