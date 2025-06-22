/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.spi;

import jakarta.persistence.AccessType;

/**
 * Common interface for JAXB bindings that represent persistent attributes.
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JaxbPersistentAttribute {
	/**
	 * The attribute's name
	 */
	String getName();
	void setName(String name);

	/**
	 * JPA's way to specify an access-strategy
	 */
	AccessType getAccess();
	void setAccess(AccessType accessType);

	/**
	 * Hibernate's pluggable access-strategy support
	 */
	String getAttributeAccessor();
	void setAttributeAccessor(String value);
}
