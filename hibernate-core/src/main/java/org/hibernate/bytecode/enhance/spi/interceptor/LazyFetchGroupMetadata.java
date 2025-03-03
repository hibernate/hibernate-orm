/*
 * SPDX-License-Identifier: Apache-2.0
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
