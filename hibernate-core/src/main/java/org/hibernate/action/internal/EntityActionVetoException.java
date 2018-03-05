/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.action.internal;
import org.hibernate.HibernateException;

/**
 * An exception indicating that an {@link org.hibernate.action.internal.EntityAction} was vetoed.
 *
 * @author Vlad Mihalcea
 */
public class EntityActionVetoException extends HibernateException {

	private final EntityAction entityAction;

	/**
	 * Constructs a EntityActionVetoException
	 *
	 * @param message Message explaining the exception condition
	 * @param entityAction The {@link org.hibernate.action.internal.EntityAction} was vetoed that was vetoed.
	 */
	public EntityActionVetoException(String message, EntityAction entityAction) {
		super( message );
		this.entityAction = entityAction;
	}

	public EntityAction getEntityAction() {
		return entityAction;
	}
}
