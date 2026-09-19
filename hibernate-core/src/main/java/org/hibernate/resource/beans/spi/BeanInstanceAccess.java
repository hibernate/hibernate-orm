/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.beans.spi;

import org.hibernate.Incubating;

/**
 * Controls how the {@link ManagedBeanRegistry} manages bean instances.
 *
 * @since 8.0
 * @author Sean Okafor
 */
@Incubating(since = "8.0", group = "bootstrap-safe-beans")
public enum BeanInstanceAccess {
	/**
	 * Obtain a shared, cached instance. Repeated calls for the
	 * same bean class return the same {@link ManagedBean} handle.
	 */
	REUSE,

	/**
	 * Obtain a fresh, independently-managed instance. Each call
	 * returns a new {@link ManagedBean} that must be released
	 * individually via {@link ManagedBeanRegistry#releaseBean}.
	 */
	DISTINCT
}
