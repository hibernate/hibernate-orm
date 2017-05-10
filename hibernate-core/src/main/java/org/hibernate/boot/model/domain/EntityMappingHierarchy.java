/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import org.hibernate.EntityMode;
import org.hibernate.engine.OptimisticLockStyle;

/**
 * @author Steve Ebersole
 */
public interface EntityMappingHierarchy {
	IdentifiableTypeMapping getRootType();

	// todo (6.0) id, version, etc here
	//		have existing "root" info methods delegate here - allows tools to keep working.

	/**
	 * Get the identifier persistent attribute mapping.
	 */
	PersistentAttributeMapping getIdentifierAttributeMapping();

	/**
	 * Get the identifier embedded value mapping.
	 */
	EmbeddedValueMapping getIdentifierEmbeddedValueMapping();

	/**
	 * Get the version attribute mapping.
	 */
	PersistentAttributeMapping getVersionAttributeMapping();

	/**
	 * Get the discriminator mapping.
	 */
	ValueMapping getDiscriminatorMapping();

	/**
	 * Get the optimistic locking style.
	 */
	OptimisticLockStyle getOptimisticLockStyle();

	boolean hasIdentifierAttributeMapping();

	boolean hasIdentifierMapper();

	boolean hasEmbeddedIdentifier();

	boolean isVersioned();

	EntityMode getEntityMode();
}
