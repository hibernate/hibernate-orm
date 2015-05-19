/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;

import org.jboss.logging.Logger;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorRegistry {
	private static final Logger log = Logger.getLogger( JavaTypeDescriptorRegistry.class );

	public static final JavaTypeDescriptorRegistry INSTANCE = new JavaTypeDescriptorRegistry();

	private ConcurrentHashMap<Class,JavaTypeDescriptor> descriptorsByClass = new ConcurrentHashMap<Class, JavaTypeDescriptor>();

	/**
	 * Adds the given descriptor to this registry
	 *
	 * @param descriptor The descriptor to add.
	 */
	public void addDescriptor(JavaTypeDescriptor descriptor) {
		descriptorsByClass.put( descriptor.getJavaTypeClass(), descriptor );
	}

	@SuppressWarnings("unchecked")
	public <T> JavaTypeDescriptor<T> getDescriptor(Class<T> cls) {
		if ( cls == null ) {
			throw new IllegalArgumentException( "Class passed to locate Java type descriptor cannot be null" );
		}

		JavaTypeDescriptor<T> descriptor = descriptorsByClass.get( cls );
		if ( descriptor != null ) {
			return descriptor;
		}

		if ( cls.isEnum() ) {
			descriptor = new EnumJavaTypeDescriptor( cls );
			descriptorsByClass.put( cls, descriptor );
			return descriptor;
		}

		if ( Serializable.class.isAssignableFrom( cls ) ) {
			return new SerializableTypeDescriptor( cls );
		}

		// find the first "assignable" match
		for ( Map.Entry<Class,JavaTypeDescriptor> entry : descriptorsByClass.entrySet() ) {
			if ( entry.getKey().isAssignableFrom( cls ) ) {
				log.debugf( "Using  cached JavaTypeDescriptor instance for Java class [%s]", cls.getName() );
				return entry.getValue();
			}
		}

		log.warnf( "Could not find matching type descriptor for requested Java class [%s]; using fallback", cls.getName() );
		return new FallbackJavaTypeDescriptor<T>( cls );
	}


	public static class FallbackJavaTypeDescriptor<T> extends AbstractTypeDescriptor<T> {
		@SuppressWarnings("unchecked")
		protected FallbackJavaTypeDescriptor(Class<T> type) {
			// MutableMutabilityPlan would be the "safest" option, but we do not necessarily know how to deepCopy etc...
			super( type, ImmutableMutabilityPlan.INSTANCE );
		}

		@Override
		public String toString(T value) {
			return value == null ? "<null>" : value.toString();
		}

		@Override
		public T fromString(String string) {
			throw new HibernateException(
					"Not known how to convert String to given type [" + getJavaTypeClass().getName() + "]"
			);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> X unwrap(T value, Class<X> type, WrapperOptions options) {
			return (X) value;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <X> T wrap(X value, WrapperOptions options) {
			return (T) value;
		}
	}

}
