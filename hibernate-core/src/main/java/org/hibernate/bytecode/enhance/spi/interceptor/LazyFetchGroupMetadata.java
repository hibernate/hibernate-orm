/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi.interceptor;

/**
 * Information about a particular bytecode lazy attribute grouping.
 *
 * @author Steve Ebersole
 */
public interface LazyFetchGroupMetadata {
	/**
	 * Access to the name of the fetch group.
	 *
	 * @return The fetch group name
	 */
	String getName();
}
