/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models;

/**
 * JPA defines 2 ways events callbacks can happen...
 *
 * @author Steve Ebersole
 */
public enum JpaEventListenerStyle {
	/**
	 * The event method is declared on the entity class.
	 * The annotated method should define no arguments and have a void return type.
	 */
	CALLBACK,

	/**
	 * The event method is declared on a separate "listener" class named by {@linkplain jakarta.persistence.EntityListeners}.
	 * The annotated method should accept a single argument - the entity instance - and have a void return type.
	 */
	LISTENER
}
