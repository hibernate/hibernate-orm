/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * An enum of the different ways a value might be "included".
 * <p>
 * This is really an expanded true/false notion with "PARTIAL" being the
 * expansion.  PARTIAL deals with components in the cases where
 * parts of the referenced component might define inclusion, but the
 * component overall does not.
 *
 * @author Steve Ebersole
 */
public enum ValueInclusion {
	NONE,
	FULL,
	PARTIAL
}
