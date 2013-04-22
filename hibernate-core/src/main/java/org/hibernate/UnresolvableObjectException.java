/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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







