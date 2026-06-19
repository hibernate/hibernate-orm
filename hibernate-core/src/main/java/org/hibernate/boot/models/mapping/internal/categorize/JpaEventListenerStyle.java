/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

/// How a JPA lifecycle callback method is declared.
///
/// The style determines the legal callback method signature.
///
/// @since 9.0
/// @author Steve Ebersole
public enum JpaEventListenerStyle {
	/// The event method is declared on the entity class.
	/// The annotated method should define no arguments and have a void return type.
	CALLBACK,

	/// The event method is declared on a separate "listener" class named by {@linkplain jakarta.persistence.EntityListeners}.
	/// The annotated method should accept a single argument - the entity instance - and have a void return type.
	LISTENER
}
