/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

import org.hibernate.pretty.MessageHelper;

/**
 * A specialized StaleStateException that carries information about the particular entity
 * instance that was the source of the failure.
 *
 * @author Gavin King
 */
public class StaleObjectStateException extends StaleStateException {
	private final String entityName;
	private final Serializable identifier;

	/**
	 * Constructs a StaleObjectStateException using the supplied information.
	 *
	 * @param entityName The name of the entity
	 * @param identifier The identifier of the entity
	 */
	public StaleObjectStateException(String entityName, Serializable identifier) {
		super( "Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)" );
		this.entityName = entityName;
		this.identifier = identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	public Serializable getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return super.getMessage() + " : " + MessageHelper.infoString( entityName, identifier );
	}

}
