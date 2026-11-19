package org.hibernate.metamodel.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.InstantiationException;
import org.hibernate.metamodel.spi.ValueAccess;

import static org.hibernate.internal.util.ReflectHelper.getRecordComponentNames;

/**
 * Support for instantiating embeddables as record representation
 */
public class EmbeddableInstantiatorRecordIndirecting extends EmbeddableInstantiatorRecordStandard {

	protected final int[] index;

	public EmbeddableInstantiatorRecordIndirecting(@Nonnull Class<?> javaType, @Nonnull int[] index) {
		super( javaType );
		this.index = index;
	}

	@Nonnull
	public static EmbeddableInstantiatorRecordIndirecting of(@Nonnull Class<?> javaType, @Nonnull String[] propertyNames) {
		final var componentNames = getRecordComponentNames( javaType );
		final var index = new int[componentNames.length];
		return EmbeddableHelper.resolveIndex( propertyNames, componentNames, index )
				? new EmbeddableInstantiatorRecordIndirectingWithGap( javaType, index )
				: new EmbeddableInstantiatorRecordIndirecting( javaType, index );
	}

	@Override
	@Nonnull
	public Object instantiate(@Nonnull ValueAccess valuesAccess) {
		if ( constructor == null ) {
			throw new InstantiationException( "Unable to locate constructor for embeddable", getMappedPojoClass() );
		}

		try {
			final var originalValues = valuesAccess.getValues();
			final var values = new Object[originalValues.length];
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
	private static class EmbeddableInstantiatorRecordIndirectingWithGap
			extends EmbeddableInstantiatorRecordIndirecting {

		public EmbeddableInstantiatorRecordIndirectingWithGap(@Nonnull Class<?> javaType, @Nonnull int[] index) {
			super( javaType, index );
		}

		@Override
		@Nonnull
		public Object instantiate(@Nonnull ValueAccess valuesAccess) {
			if ( constructor == null ) {
				throw new InstantiationException( "Unable to locate constructor for embeddable", getMappedPojoClass() );
			}

			try {
				final var originalValues = valuesAccess.getValues();
				final var values = new Object[index.length];
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
