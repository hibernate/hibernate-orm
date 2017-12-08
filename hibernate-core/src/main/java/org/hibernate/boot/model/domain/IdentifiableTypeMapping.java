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

	/**
	 * @todo (6.0) Should we order these?
	 * 		I'm just not sure there is a clear benefit here (beyond root first), so at the moment
	 * 		I'd lean towards no.
	 */
	Collection<IdentifiableTypeMapping> getSubTypeMappings();

	/**
	 * It is conceivable that a user might want to define a
	 * SecondaryTable on a MappedSuperclass, so we add those here.
	 */
	Collection<MappedTableJoin> getSecondaryTables();

	/**
	 * Get the attribute that represents a single identifier or an embedded id.
	 */
	PersistentAttributeMapping getIdentifierAttributeMapping();

	/**
	 * Get the mapping associated to multiple-id or {@link javax.persistence.IdClass}.
	 */
	EmbeddedValueMapping getEmbeddedIdentifierAttributeMapping();

	/**
	 * Get the declared mapping associated to multiple-id or {@link javax.persistence.IdClass}.
	 */
	EmbeddedValueMapping getDeclaredEmbeddedIdentifierAttributeMapping();

	/**
	 * Get the version attribute.
	 */
	PersistentAttributeMapping getVersionAttributeMapping();

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
	boolean hasVersionAttributeMapping();

	/**
	 * Checks whether the identifier attribute represents a single id or embedded id.
	 */
	boolean hasSingleIdentifierAttributeMapping();
}
