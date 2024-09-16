/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
