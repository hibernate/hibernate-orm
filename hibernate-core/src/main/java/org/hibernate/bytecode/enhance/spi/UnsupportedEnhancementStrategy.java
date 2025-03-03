/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.spi;

import org.hibernate.Incubating;

/**
 * The expected behavior when encountering a class that cannot be enhanced,
 * in particular when attribute names don't match field names.
 *
 * @see org.hibernate.bytecode.enhance.spi.EnhancementContext#getUnsupportedEnhancementStrategy
 */
@Incubating
public enum UnsupportedEnhancementStrategy {

	/**
	 * When a class cannot be enhanced, skip enhancement for that class only.
	 */
	SKIP,
	/**
	 * When a class cannot be enhanced, throw an exception with an actionable message.
	 */
	FAIL,
	/**
	 * Legacy behavior: when a class cannot be enhanced, ignore that fact and try to enhance it anyway.
	 * <p>
	 * <strong>This is utterly unsafe and may cause errors, unpredictable behavior, and data loss.</strong>
	 * <p>
	 * Intended only for internal use in contexts with rigid backwards compatibility requirements.
	 *
	 * @deprecated Use {@link #SKIP} or {@link #FAIL} instead.
	 */
	@Deprecated
	LEGACY

}
