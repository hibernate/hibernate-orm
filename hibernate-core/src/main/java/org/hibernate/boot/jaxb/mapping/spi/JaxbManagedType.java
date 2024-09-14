/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.mapping.spi;

import jakarta.persistence.AccessType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Common interface for JAXB bindings representing entities, mapped-superclasses and embeddables (JPA collective
 * calls these "managed types" in terms of its Metamodel api).
 *
 * @author Strong Liu
 * @author Steve Ebersole
 */
public interface JaxbManagedType {

	@Nullable String getDescription();

	void setDescription(@Nullable String value);

	@Nullable String getClazz();

	void setClazz(@Nullable String className);

	@Nullable Boolean isMetadataComplete();

	void setMetadataComplete(@Nullable Boolean isMetadataComplete);

	@Nullable AccessType getAccess();

	void setAccess(@Nullable AccessType value);

	@Nullable JaxbAttributesContainer getAttributes();
}
