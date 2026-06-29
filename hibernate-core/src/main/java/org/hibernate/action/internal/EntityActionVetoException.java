/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;

import jakarta.annotation.Nonnull;

import org.hibernate.HibernateException;

/**
 * An exception indicating that an {@link EntityAction} was vetoed.
 *
 * @author Vlad Mihalcea
 */
public class EntityActionVetoException extends HibernateException {

	private final EntityAction entityAction;

	/**
	 * Constructs an {@code EntityActionVetoException}
	 *
	 * @param message Message explaining the exception condition
	 * @param entityAction The {@link EntityAction} was vetoed that was vetoed
	 */
	public EntityActionVetoException(@Nonnull String message, @Nonnull EntityAction entityAction) {
		super( message );
		this.entityAction = entityAction;
	}

	public EntityActionVetoException(@Nonnull EntityAction entityAction) {
		super( "Action was vetoed: " + entityAction );
		this.entityAction = entityAction;
	}

	@Nonnull
	public EntityAction getEntityAction() {
		return entityAction;
	}
}
