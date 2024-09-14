/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
