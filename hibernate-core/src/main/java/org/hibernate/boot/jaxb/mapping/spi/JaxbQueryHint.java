/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

/**
 * Models a named query hint in the JAXB model
 *
 * @author Steve Ebersole
 */
public interface JaxbQueryHint {
	/**
	 * The hint name.
	 *
	 * @see org.hibernate.jpa.AvailableHints
	 */
	String getName();

	/**
	 * The hint value.
	 */
	String getValue();
}
