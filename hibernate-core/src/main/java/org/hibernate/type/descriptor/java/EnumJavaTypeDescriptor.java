/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.type.descriptor.WrapperOptions;

/**
 * Describes a Java Enum type.
 *
 * @author Steve Ebersole
 */
public class EnumJavaTypeDescriptor<T extends Enum> extends AbstractTypeDescriptor<T> {
	@SuppressWarnings("unchecked")
	protected EnumJavaTypeDescriptor(Class<T> type) {
		super( type, ImmutableMutabilityPlan.INSTANCE );

		JavaTypeDescriptorRegistry.INSTANCE.addDescriptor( this );
	}

	@Override
	public String toString(T value) {
		return value == null ? "<null>" : value.name();
	}

	@Override
	public T fromString(String string) {
		return string == null ? null : (T) Enum.valueOf( getJavaTypeClass(), string );
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
