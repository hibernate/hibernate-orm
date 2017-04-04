/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.io.Serializable;

/**
 * Thrown when <tt>Session.load()</tt> fails to select a row with
 * the given primary key (identifier value). This exception might not
 * be thrown when <tt>load()</tt> is called, even if there was no
 * row on the database, because <tt>load()</tt> returns a proxy if
 * possible. Applications should use <tt>Session.get()</tt> to test if
 * a row exists in the database.<br>
 * <br> 
 * Like all Hibernate exceptions, this exception is considered 
 * unrecoverable.
 *
 * @author Gavin King
 */
public class ObjectNotFoundException extends UnresolvableObjectException {
	/**
	 * Constructs a ObjectNotFoundException using the given information.
	 *
	 * @param identifier The identifier of the entity
	 * @param entityName The name of the entity
	 */
	public ObjectNotFoundException(Serializable identifier, String entityName) {
		super( identifier, entityName );
	}
}
