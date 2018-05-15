/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import java.util.Collection;

/**
 * @author Steve Ebersole
 */
public interface IdentifiableTypeMapping extends ManagedTypeMapping {
	EntityMappingHierarchy getEntityMappingHierarchy();

	IdentifiableTypeMapping getSuperTypeMapping();

	@Override
	IdentifiableJavaTypeMapping getJavaTypeMapping();

	Collection<IdentifiableTypeMapping> getSubTypeMappings();

	/**
	 * It is conceivable that a user might want to define a
	 * SecondaryTable on a MappedSuperclass, so we add those here.
	 */
	Collection<MappedJoin> getMappedJoins();

	/**
	 * Get the attribute that represents a single identifier or an embedded id.
	 */
	default PersistentAttributeMapping getIdentifierAttributeMapping() {
		return getEntityMappingHierarchy().getIdentifierAttributeMapping();
	}

	/**
	 * Get the mapping associated to multiple-id or {@link javax.persistence.IdClass}.
	 */
	default EmbeddedValueMapping getEmbeddedIdentifierAttributeMapping() {
		return getEntityMappingHierarchy().getIdentifierEmbeddedValueMapping();
	}

	/**
	 * Get the declared mapping associated to multiple-id or {@link javax.persistence.IdClass}.
	 */
	EmbeddedValueMapping getDeclaredEmbeddedIdentifierAttributeMapping();

	/**
	 * Get the version attribute.
	 */
	default PersistentAttributeMapping getVersionAttributeMapping() {
		return getEntityMappingHierarchy().getVersionAttributeMapping();
	}

	/**
	 * Get the locally declared attribute that represents a single identifier
	 * or an embedded id.
	 */
	PersistentAttributeMapping getDeclaredIdentifierAttributeMapping();

	/**
	 * Get the locally declared version attribute.
	 */
	PersistentAttributeMapping getDeclaredVersionAttributeMapping();

	/**
	 * Checks whether a version attribute mapping has been specified.
	 */
	default boolean hasVersionAttributeMapping() {
		return getEntityMappingHierarchy().hasVersionAttributeMapping();
	}

	/**
	 * Checks whether the identifier attribute represents a single id or embedded id.
	 */
	default boolean hasSingleIdentifierAttributeMapping() {
		return getEntityMappingHierarchy().hasIdentifierAttributeMapping();
	}
}
