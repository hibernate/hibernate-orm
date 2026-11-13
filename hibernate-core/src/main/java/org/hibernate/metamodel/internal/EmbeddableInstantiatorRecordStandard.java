package org.hibernate.metamodel.internal;

import org.hibernate.InstantiationException;
import org.hibernate.accessor.AccessorFactory;
import org.hibernate.accessor.Instantiator;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

import static org.hibernate.internal.util.ReflectHelper.getConstructorOrNull;
import static org.hibernate.internal.util.ReflectHelper.getRecordComponentTypes;

/**
 * Support for instantiating embeddables as record representation
 */
public class EmbeddableInstantiatorRecordStandard extends AbstractPojoInstantiator implements EmbeddableInstantiator {

	protected final Instantiator<?> instantiator;

	public EmbeddableInstantiatorRecordStandard(Class<?> javaType, AccessorFactory accessorFactory) {
		super( javaType );
		final var constructor = getConstructorOrNull( javaType, getRecordComponentTypes( javaType ) );
		instantiator = constructor == null ? null : accessorFactory.instantiator( constructor );
	}

	@Override
	public Object instantiate(ValueAccess valuesAccess) {
		if ( instantiator == null ) {
			throw new InstantiationException( "Unable to locate constructor for embeddable", getMappedPojoClass() );
		}

		try {
			return instantiator.create( valuesAccess.getValues() );
		}
		catch ( Exception e ) {
			throw new InstantiationException( "Could not instantiate entity", getMappedPojoClass(), e );
		}
	}
}
