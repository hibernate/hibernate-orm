/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.internal;
import org.hibernate.HibernateException;

/**
 * An exception indicating that an {@link EntityAction} was vetoed.
 *
 * @author Vlad Mihalcea
 */
public class EntityActionVetoException extends HibernateException {

	private final EntityAction entityAction;

	/**
	 * Constructs a EntityActionVetoException
	 *
	 * @param message Message explaining the exception condition
	 * @param entityAction The {@link EntityAction} was vetoed that was vetoed.
	 */
	public EntityActionVetoException(String message, EntityAction entityAction) {
		super( message );
		this.entityAction = entityAction;
	}

	public EntityAction getEntityAction() {
		return entityAction;
	}
}
