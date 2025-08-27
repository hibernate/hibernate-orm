/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

/**
 * Used by {@link BytecodeEnhancerRunner} to determine which classes should
 * be enhanced.
 *
 * @author Steve Ebersole
 */
public interface EnhancementSelector {
	/**
	 * Determine whether the named class should be enhanced.
	 */
	boolean select(String name);
}
