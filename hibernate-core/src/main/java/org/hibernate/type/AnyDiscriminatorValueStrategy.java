/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.Incubating;
import org.hibernate.annotations.AnyDiscriminatorValue;

/**
 * Describes how to deal with discriminator values in regard to
 * a {@linkplain org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart ANY mapping}
 *
 * @see AnyDiscriminatorValue
 *
 * @since 7.0
 * @author Steve Ebersole
 */
@Incubating
public enum AnyDiscriminatorValueStrategy {
	/**
	 * Pick between {@link #IMPLICIT} and {@link #EXPLICIT} based on
	 * presence or not of {@link AnyDiscriminatorValue}.  The default
	 * (and legacy) behavior.
	 */
	AUTO,

	/**
	 * Expect explicit, complete mapping of discriminator values using
	 * one-or-more {@link AnyDiscriminatorValue}.
	 *
	 * @implNote With this option, it is considered an error if, at runtime,
	 * we encounter an entity type not explicitly mapped with a
	 * {@link AnyDiscriminatorValue}.
	 */
	EXPLICIT,

	/**
	 * Expect no {@link AnyDiscriminatorValue}.  The entity name of the associated
	 * entity is used as the discriminator value.
	 *
	 * @implNote With this option, it is considered illegal to specify
	 * any {@link AnyDiscriminatorValue} mappings.
	 */
	IMPLICIT,

	/**
	 * Allows a combination of {@linkplain #EXPLICIT explicit} and {@linkplain #IMPLICIT implicit}
	 * discriminator value mappings.  If an entity is mapped using an explicit
	 * {@link AnyDiscriminatorValue} mapping, the associated discriminator value is used.
	 * Otherwise, the entity name is used.
	 *
	 * @implNote This option is roughly the same as {@link #EXPLICIT} except
	 * that an implicit mapping using the entity name is used when a matching
	 * explicit {@link AnyDiscriminatorValue} mapping is not found.
	 */
	MIXED
}
