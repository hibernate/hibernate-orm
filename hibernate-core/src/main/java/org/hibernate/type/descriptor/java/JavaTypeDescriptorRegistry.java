/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Basically a map from {@link Class} -> {@link JavaTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public class JavaTypeDescriptorRegistry {
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

		// find the first "assignable" match
		for ( Map.Entry<Class,JavaTypeDescriptor> entry : descriptorsByClass.entrySet() ) {
			if ( cls.isAssignableFrom( entry.getKey() ) ) {
				return entry.getValue();
			}
		}

		// we could not find one; warn the user (as stuff is likely to break later) and create a fallback instance...
		if ( Serializable.class.isAssignableFrom( cls ) ) {
			return new SerializableTypeDescriptor( cls );
		}
		else {
			return new FallbackJavaTypeDescriptor<T>( cls );
		}
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
