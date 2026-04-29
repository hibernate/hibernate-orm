/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

/// Distinction between the 2 different styles or approaches of lifecycle events defined by JPA.
///
/// @author Steve Ebersole
public enum JpaEventListenerStyle {
	/// The lifecycle method is declared on the entity class.
	/// The annotated method should define no arguments and have a void return type.
	CALLBACK,

	/// The lifecycle method is declared on a separate "listener" class.
	/// The annotated method should accept a single argument - the entity instance - and have a void return type.
	///
	/// @see jakarta.persistence.EntityListeners
	/// @see jakarta.persistence.EntityListener
	LISTENER
}
