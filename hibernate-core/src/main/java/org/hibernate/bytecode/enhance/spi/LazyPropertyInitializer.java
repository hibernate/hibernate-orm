/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import org.hibernate.bytecode.enhance.spi.interceptor.BytecodeLazyAttributeInterceptor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Contract for controlling how lazy properties get initialized.
 *
 * @author Gavin King
 */
public interface LazyPropertyInitializer {

	/**
	 * Marker value for uninitialized properties.
	 */
	Serializable UNFETCHED_PROPERTY = new Serializable() {
		@Override
		public String toString() {
			return "<lazy>";
		}

		public Object readResolve() {
			return UNFETCHED_PROPERTY;
		}
	};

	/**
	 * @deprecated Prefer the form of these methods defined on
	 * {@link BytecodeLazyAttributeInterceptor} instead
	 */
	@Deprecated
	interface InterceptorImplementor {
		default Set<String> getInitializedLazyAttributeNames() {
			return Collections.emptySet();
		}

		default void attributeInitialized(String name) {
		}
	}

	/**
	 * Initialize the property, and return its new value.
	 *
	 * @param fieldName The name of the field being initialized
	 * @param entity The entity on which the initialization is occurring
	 * @param session The session from which the initialization originated.
	 *
	 * @return ?
	 */
	Object initializeLazyProperty(String fieldName, Object entity, SharedSessionContractImplementor session);

}
