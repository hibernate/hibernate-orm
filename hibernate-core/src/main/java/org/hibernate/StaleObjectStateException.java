/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import static org.hibernate.pretty.MessageHelper.infoString;

/**
 * A specialized {@link StaleStateException} that carries information about
 * the particular entity instance that was the source of the failure.
 *
 * @author Gavin King
 */
public class StaleObjectStateException extends StaleStateException {
	private final String entityName;
	private final Object identifier;

	/**
	 * Constructs a {@code StaleObjectStateException} using the supplied information.
	 *
	 * @param entityName The name of the entity
	 * @param identifier The identifier of the entity
	 */
	public StaleObjectStateException(String entityName, Object identifier) {
		this( entityName, identifier, "Row was already updated or deleted by another transaction" );
	}

	/**
	 * Constructs a {@code StaleObjectStateException} using the supplied information.
	 *
	 * @param entityName The name of the entity
	 * @param identifier The identifier of the entity
	 */
	public StaleObjectStateException(String entityName, Object identifier, String message) {
		super( message );
		this.entityName = entityName;
		this.identifier = identifier;
	}

	/**
	 * Constructs a {@code StaleObjectStateException} using the supplied information
	 * and cause.
	 *
	 * @param entityName The name of the entity
	 * @param identifier The identifier of the entity
	 */
	public StaleObjectStateException(String entityName, Object identifier, StaleStateException cause) {
		super( cause.getMessage(), cause );
		this.entityName = entityName;
		this.identifier = identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	public Object getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return super.getMessage() + " for entity " + infoString( entityName, identifier );
	}

}
