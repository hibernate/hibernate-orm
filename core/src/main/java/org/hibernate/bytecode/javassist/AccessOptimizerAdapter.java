package org.hibernate.bytecode.javassist;

import org.hibernate.bytecode.ReflectionOptimizer;
import org.hibernate.PropertyAccessException;

import java.io.Serializable;

/**
 * The {@link ReflectionOptimizer.AccessOptimizer} implementation for Javassist
 * which simply acts as an adpater to the {@link BulkAccessor} class.
 *
 * @author Steve Ebersole
 */
public class AccessOptimizerAdapter implements ReflectionOptimizer.AccessOptimizer, Serializable {

	public static final String PROPERTY_GET_EXCEPTION =
		"exception getting property value with Javassist (set hibernate.bytecode.use_reflection_optimizer=false for more info)";

	public static final String PROPERTY_SET_EXCEPTION =
		"exception setting property value with Javassist (set hibernate.bytecode.use_reflection_optimizer=false for more info)";

	private final BulkAccessor bulkAccessor;
	private final Class mappedClass;

	public AccessOptimizerAdapter(BulkAccessor bulkAccessor, Class mappedClass) {
		this.bulkAccessor = bulkAccessor;
		this.mappedClass = mappedClass;
	}

	public String[] getPropertyNames() {
		return bulkAccessor.getGetters();
	}

	public Object[] getPropertyValues(Object object) {
		try {
			return bulkAccessor.getPropertyValues( object );
		}
		catch ( Throwable t ) {
			throw new PropertyAccessException(
					t,
			        PROPERTY_GET_EXCEPTION,
			        false,
			        mappedClass,
			        getterName( t, bulkAccessor )
				);
		}
	}

	public void setPropertyValues(Object object, Object[] values) {
		try {
			bulkAccessor.setPropertyValues( object, values );
		}
		catch ( Throwable t ) {
			throw new PropertyAccessException(
					t,
			        PROPERTY_SET_EXCEPTION,
			        true,
			        mappedClass,
			        setterName( t, bulkAccessor )
			);
		}
	}

	private static String setterName(Throwable t, BulkAccessor accessor) {
		if (t instanceof BulkAccessorException ) {
			return accessor.getSetters()[ ( (BulkAccessorException) t ).getIndex() ];
		}
		else {
			return "?";
		}
	}

	private static String getterName(Throwable t, BulkAccessor accessor) {
		if (t instanceof BulkAccessorException ) {
			return accessor.getGetters()[ ( (BulkAccessorException) t ).getIndex() ];
		}
		else {
			return "?";
		}
	}
}
