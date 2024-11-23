/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import javax.persistence.AccessType;

/**
 * Common interface for JAXB bindings representing entities, mapped-superclasses and embeddables (JPA collective
 * calls these "managed types" in terms of its Metamodel api).
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface ManagedType {

	String getDescription();

	void setDescription(String value);

	String getClazz();

	void setClazz(String className);

	Boolean isMetadataComplete();

	void setMetadataComplete(Boolean isMetadataComplete);

	AccessType getAccess();

	void setAccess(AccessType value);

	AttributesContainer getAttributes();
}
