/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import org.hibernate.boot.model.domain.spi.IdentifiableTypeMappingImplementor;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.model.domain.RepresentationMode;

/**
 * @author Steve Ebersole
 */
public interface EntityMappingHierarchy {
	IdentifiableTypeMappingImplementor getRootType();

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

	/**
	 * Checks whether a single id or embedded id attribute mapping has been specified.
	 */
	boolean hasIdentifierAttributeMapping();

	boolean hasIdentifierMapper();

	/**
	 * Returns true if the hierarchy has multiple id annotations specified without an
	 * actual {@link javax.persistence.IdClass} implementation.
	 */
	boolean hasEmbeddedIdentifier();

	/**
	 * Checks whether a version attribute mapping has been specified.
	 */
	boolean hasVersionAttributeMapping();

	/**
	 * Get the Representation-mode being used by this hierarchy.
	 */
	RepresentationMode getExplicitRepresentationMode();
}
