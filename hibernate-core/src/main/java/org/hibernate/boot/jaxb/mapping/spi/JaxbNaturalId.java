/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * JAXB binding interface for natural-id definitions
 *
 * @author Steve Ebersole
 */
public interface JaxbNaturalId extends JaxbBaseAttributesContainer {
	/**
	 * The cache config associated with this natural-id
	 */
	JaxbCachingImpl getCaching();

	/**
	 * Whether the natural-id (all attributes which are part of it) should
	 * be considered mutable or immutable.
	 */
	boolean isMutable();
}
