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
 * Thrown when Hibernate could not resolve an object by id, especially when
 * loading an association.
 *
 * @author Gavin King
 */
public class UnresolvableObjectException extends HibernateException {
	private final Serializable identifier;
	private final String entityName;

	/**
	 * Constructs an UnresolvableObjectException using the specified information.
	 *
	 * @param identifier The identifier of the entity which could not be resolved
	 * @param entityName The name of the entity which could not be resolved
	 */
	public UnresolvableObjectException(Serializable identifier, String entityName) {
		this( "No row with the given identifier exists", identifier, entityName );
	}

	protected UnresolvableObjectException(String message, Serializable identifier, String clazz) {
		super( message );
		this.identifier = identifier;
		this.entityName = clazz;
	}

	/**
	 * Factory method for building and throwing an UnresolvableObjectException if the entity is null.
	 *
	 * @param entity The entity to check for nullness
	 * @param identifier The identifier of the entity
	 * @param entityName The name of the entity
	 *
	 * @throws UnresolvableObjectException Thrown if entity is null
	 */
	public static void throwIfNull(Object entity, Serializable identifier, String entityName)
			throws UnresolvableObjectException {
		if ( entity == null ) {
			throw new UnresolvableObjectException( identifier, entityName );
		}
	}

	public Serializable getIdentifier() {
		return identifier;
	}

	public String getEntityName() {
		return entityName;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + ": " + MessageHelper.infoString( entityName, identifier );
	}

}
