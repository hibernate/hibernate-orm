/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Constructor;

import org.hibernate.InstantiationException;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * Support for instantiating embeddables as POJO representation through a constructor
 */
public class EmbeddableInstantiatorPojoIndirecting extends AbstractPojoInstantiator implements EmbeddableInstantiator {
	protected final Constructor<?> constructor;
	protected final int[] index;

	protected EmbeddableInstantiatorPojoIndirecting(Constructor<?> constructor, int[] index) {
		super( constructor.getDeclaringClass() );
		this.constructor = constructor;
		this.index = index;
	}

	public static EmbeddableInstantiatorPojoIndirecting of(
			String[] propertyNames,
			Constructor<?> constructor,
			String[] componentNames) {
		if ( componentNames == null ) {
			throw new IllegalArgumentException( "Can't determine field assignment for constructor: " + constructor );
		}
		final int[] index = new int[componentNames.length];
		return EmbeddableHelper.resolveIndex( propertyNames, componentNames, index )
				? new EmbeddableInstantiatorPojoIndirectingWithGap( constructor, index )
				: new EmbeddableInstantiatorPojoIndirecting( constructor, index );
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		try {
			final Object[] originalValues = valuesAccess.getValues();
			final Object[] values = new Object[originalValues.length];
			for ( int i = 0; i < values.length; i++ ) {
				values[i] = originalValues[index[i]];
			}
			return constructor.newInstance( values );
		}
		catch ( Exception e ) {
			throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
		}
	}

	// Handles gaps, by leaving the value null for that index
	private static class EmbeddableInstantiatorPojoIndirectingWithGap extends EmbeddableInstantiatorPojoIndirecting {

		public EmbeddableInstantiatorPojoIndirectingWithGap(Constructor<?> constructor, int[] index) {
			super( constructor, index );
		}

		@Override
		public Object instantiate(ValueAccess valuesAccess) {
			try {
				final Object[] originalValues = valuesAccess.getValues();
				final Object[] values = new Object[index.length];
				for ( int i = 0; i < values.length; i++ ) {
					final int index = this.index[i];
					if ( index >= 0 ) {
						values[i] = originalValues[index];
					}
				}
				return constructor.newInstance( values );
			}
			catch ( Exception e ) {
				throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
			}
		}
	}
}
