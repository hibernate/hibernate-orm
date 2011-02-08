/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate;
import java.io.Serializable;

/**
 * Thrown when <tt>Session.load()</tt> selects a row with
 * the given primary key (identifier value) but the row's
 * discriminator value specifies a subclass that is not
 * assignable to the class requested by the user.
 *
 * @author Gavin King
 */
public class WrongClassException extends HibernateException {

	private final Serializable identifier;
	private final String entityName;

	public WrongClassException(String msg, Serializable identifier, String clazz) {
		super(msg);
		this.identifier = identifier;
		this.entityName = clazz;
	}
	public Serializable getIdentifier() {
		return identifier;
	}

	public String getMessage() {
		return "Object with id: " +
			identifier +
			" was not of the specified subclass: " +
			entityName +
			" (" + super.getMessage() + ")" ;
	}

	public String getEntityName() {
		return entityName;
	}

}







