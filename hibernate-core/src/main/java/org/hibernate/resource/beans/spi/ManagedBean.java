/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

/**
 * Generalized contract for a (CDI or Spring) "managed bean" as seen by Hibernate
 *
 * @author Steve Ebersole
 */
public interface ManagedBean<T> {
	/**
	 * The bean Java type
	 */
	Class<T> getBeanClass();

	/**
	 * The bean reference
	 */
	T getBeanInstance();
}
