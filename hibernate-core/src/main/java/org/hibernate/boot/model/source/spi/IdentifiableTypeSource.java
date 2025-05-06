/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.Collection;
import java.util.List;

import org.hibernate.boot.jaxb.Origin;

/**
 * Common contract between Entity and MappedSuperclass sources.  The
 * terminology is taken from JPA's {@link jakarta.persistence.metamodel.IdentifiableType}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeSource extends AttributeSourceContainer {
	/**
	 * Obtain the origin of this source.
	 *
	 * @return The origin of this source.
	 */
	Origin getOrigin();

	/**
	 * Get the hierarchy this belongs to.
	 *
	 * @return The hierarchy this belongs to.
	 */
	EntityHierarchySource getHierarchy();

	/**
	 * Obtain the metadata-building context local to this entity source.
	 *
	 * @return The local binding context
	 */
	LocalMetadataBuildingContext getLocalMetadataBuildingContext();

	/**
	 * Get the name of this type.
	 *
	 * @return The name of this type.
	 */
	String getTypeName();

	IdentifiableTypeSource getSuperType();

	/**
	 * Access the subtype sources for types extending from this type source,
	 *
	 * @return Subtype sources
	 */
	Collection<IdentifiableTypeSource> getSubTypes();

	/**
	 * Access to the sources describing JPA lifecycle callbacks.
	 *
	 * @return JPA lifecycle callback sources
	 */
	List<JpaCallbackSource> getJpaCallbackClasses();
}
