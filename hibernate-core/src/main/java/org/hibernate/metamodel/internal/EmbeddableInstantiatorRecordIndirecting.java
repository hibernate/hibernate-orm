/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.internal;

import org.hibernate.InstantiationException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.spi.ValueAccess;

/**
 * Support for instantiating embeddables as record representation
 */
public class EmbeddableInstantiatorRecordIndirecting extends EmbeddableInstantiatorRecordStandard {

	protected final int[] index;

	public EmbeddableInstantiatorRecordIndirecting(Class<?> javaType, int[] index) {
		super( javaType );
		this.index = index;
	}

	public static EmbeddableInstantiatorRecordIndirecting of(Class<?> javaType, String[] propertyNames) {
		final String[] componentNames = ReflectHelper.getRecordComponentNames( javaType );
		final int[] index = new int[componentNames.length];
		if ( EmbeddableHelper.resolveIndex( propertyNames, componentNames, index ) ) {
			return new EmbeddableInstantiatorRecordIndirectingWithGap( javaType, index );
		}
		else {
			return new EmbeddableInstantiatorRecordIndirecting(javaType, index);
		}
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess, SessionFactoryImplementor sessionFactory) {
		if ( constructor == null ) {
			throw new InstantiationException( "Unable to locate constructor for embeddable", getMappedPojoClass() );
		}

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
	private static class EmbeddableInstantiatorRecordIndirectingWithGap extends EmbeddableInstantiatorRecordIndirecting {

		public EmbeddableInstantiatorRecordIndirectingWithGap(Class<?> javaType, int[] index) {
			super( javaType, index );
		}

		@Override
		public Object instantiate(ValueAccess valuesAccess, SessionFactoryImplementor sessionFactory) {
			if ( constructor == null ) {
				throw new InstantiationException( "Unable to locate constructor for embeddable", getMappedPojoClass() );
			}

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
