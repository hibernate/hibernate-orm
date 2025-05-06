/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.metamodel.spi.ManagedTypeRepresentationStrategy;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines how a given persistent attribute is accessed by exposing
 * a {@link Getter} and a {@link Setter} for the attribute.
 * <p>
 * Instances are obtained from a {@link PropertyAccessStrategy}.
 *
 * @see ManagedTypeRepresentationStrategy
 *
 * @author Steve Ebersole
 * @author Gavin King
 */
public interface PropertyAccess {
	/**
	 * Access to the {@link PropertyAccessStrategy} that created this instance.
	 *
	 * @return The {@code PropertyAccessStrategy}
	 */
	PropertyAccessStrategy getPropertyAccessStrategy();

	/**
	 * Obtain the delegate for getting values of the persistent attribute.
	 *
	 * @return The property getter
	 */
	Getter getGetter();

	/**
	 * Obtain the delegate for setting values of the persistent attribute.
	 *
	 * @return The property setter
	 */
	@Nullable Setter getSetter();
}
