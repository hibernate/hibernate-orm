/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import java.util.Locale;

/**
 * Thrown when loading an entity (by identifier) results in a value that
 * cannot be treated as the subclass type requested by the caller.
 *
 * @author Gavin King
 */
public class WrongClassException extends HibernateException {
	private final Object identifier;
	private final String entityName;

	/**
	 * Constructs a {@code WrongClassException} using the supplied information.
	 *  @param message A message explaining the exception condition
	 * @param identifier The identifier of the entity
	 * @param entityName The entity-type requested
	 */
	public WrongClassException(String message, Object identifier, String entityName) {
		super(
				String.format(
						"Object [id=%s] was not of the specified subclass [%s] : %s",
						identifier,
						entityName,
						message
				)
		);

		this.identifier = identifier;
		this.entityName = entityName;
	}

	public WrongClassException(String resolvedEntityName, Object identifier, String expectedEntityName, Object discriminatorValue) {
		super(
				String.format(
						Locale.ROOT,
						"Expected object of type `%s`, but found `%s`; discriminator = %s",
						resolvedEntityName,
						expectedEntityName,
						discriminatorValue
				)
		);

		this.identifier = identifier;
		this.entityName = expectedEntityName;
	}

	public String getEntityName() {
		return entityName;
	}

	public Object getIdentifier() {
		return identifier;
	}
}
