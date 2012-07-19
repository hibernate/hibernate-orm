/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.internal.util;

/**
 * Represents a "final" value that is initialized either {@link #ValueHolder(Object) up front} or once at some point
 * {@link #ValueHolder(ValueHolder.DeferredInitializer) after} declaration.
 *
 * @author Steve Ebersole
 */
public class ValueHolder<T> {

	/**
	 * The snippet that generates the initialization value.
	 *
	 * @param <T>
	 */
	public static interface DeferredInitializer<T> {
		/**
		 * Build the initialization value.
		 * <p/>
		 * Implementation note: returning {@code null} is "ok" but will cause this method to keep being called.
		 *
		 * @return The initialization value.
		 */
		public T initialize();
	}

	private final DeferredInitializer<T> valueInitializer;
	private T value;

	/**
	 * Instantiates a {@link ValueHolder} with the specified initializer.
	 *
	 * @param valueInitializer The initializer to use in {@link #getValue} when value not yet known.
	 */
	public ValueHolder(DeferredInitializer<T> valueInitializer) {
		this.valueInitializer = valueInitializer;
	}

	@SuppressWarnings( {"unchecked"})
	public ValueHolder(T value) {
		this( NO_DEFERRED_INITIALIZER );
		this.value = value;
	}

	public T getValue() {
		if ( value == null ) {
			value = valueInitializer.initialize();
		}
		return value;
	}

	private static final DeferredInitializer NO_DEFERRED_INITIALIZER = new DeferredInitializer() {
		@Override
		public Void initialize() {
			return null;
		}
	};
}
