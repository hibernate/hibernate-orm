/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.instrumentation.spi;

import java.io.Serializable;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * Contract for controlling how lazy properties get initialized.
 * 
 * @author Gavin King
 */
public interface LazyPropertyInitializer {

	/**
	 * Marker value for uninitialized properties.
	 */
	public static final Serializable UNFETCHED_PROPERTY = new Serializable() {
		@Override
		public String toString() {
			return "<lazy>";
		}

		public Object readResolve() {
			return UNFETCHED_PROPERTY;
		}
	};

	/**
	 * Initialize the property, and return its new value.
	 *
	 * @param fieldName The name of the field being initialized
	 * @param entity The entity on which the initialization is occurring
	 * @param session The session from which the initialization originated.
	 *
	 * @return ?
	 */
	public Object initializeLazyProperty(String fieldName, Object entity, SessionImplementor session);

}
