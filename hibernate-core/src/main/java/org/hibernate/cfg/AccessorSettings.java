/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

import org.hibernate.Incubating;

/**
 * Settings related to the property accessor strategy.
 */
@Incubating(since = "8.0")
public interface AccessorSettings {

	/**
	 * Specifies the accessor strategy to use for property access.
	 * <ul>
	 *     <li>{@code "generated"} -- generates bytecode via ByteBuddy for both
	 *         individual and multi-value accessors (default)</li>
	 *     <li>{@code "reflection"} -- uses plain {@code java.lang.reflect} for
	 *         all accessors, including multi-value</li>
	 * </ul>
	 *
	 * @settingDefault {@code "generated"}
	 */
	@Incubating(since = "8.0")
	String ACCESSOR_STRATEGY = "hibernate.accessor.strategy";
}
