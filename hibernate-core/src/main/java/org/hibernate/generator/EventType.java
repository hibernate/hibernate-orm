/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

/**
 * Enumerates event types that can result in generation of a new value.
 * A {@link Generator} must specify which events it responds to, by
 * implementing {@link Generator#getEventTypes()}.
 * <p>
 * We usually work with {@linkplain EventTypeSets sets} of event types,
 * even though there are only two types.
 *
 * @author Gavin King
 *
 * @since 6.2
 *
 * @see Generator
 * @see org.hibernate.annotations.Generated
 * @see org.hibernate.annotations.CurrentTimestamp
 * @see EventTypeSets
 */
public enum EventType {
	/**
	 * An event that occurs when any {@code insert} statements needed
	 * to persist a new entity instance are executed. This indicates,
	 * for example, that a surrogate primary key should be generated,
	 * or initial that an initial version number should be seeded.
	 */
	INSERT,
	/**
	 * An event that occurs when any {@code update} statements needed
	 * to persist changes to a dirty entity instance are executed.
	 * This indicates, for example, that a version number should be
	 * incremented.
	 */
	UPDATE
}
