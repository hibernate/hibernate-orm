/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A lazily accessible object reference.  Useful for cases where final references
 * are needed (anon inner class, lambdas, etc).
 *
 * @param <T> The type of object referenced
 */
public class LazyValue<T> {
	public static final Object NULL = new Object();

	private final Supplier<T> supplier;
	private Object value;

	public LazyValue(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	public T getValue() {
		if ( value == null ) {
			final T obtainedValue = supplier.get();
			value = Objects.requireNonNullElse( obtainedValue, NULL );
		}

		//noinspection unchecked
		return value == NULL ? null : (T) value;
	}
}
