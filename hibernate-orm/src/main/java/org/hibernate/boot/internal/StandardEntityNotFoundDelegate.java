/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.io.Serializable;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.proxy.EntityNotFoundDelegate;

/**
 * Standard non-JPA implementation of EntityNotFoundDelegate, throwing the
 * Hibernate-specific {@link ObjectNotFoundException}.
 *
 * @author Steve Ebersole
 */
public class StandardEntityNotFoundDelegate implements EntityNotFoundDelegate {
	/**
	 * Singleton access
	 */
	public static final StandardEntityNotFoundDelegate INSTANCE = new StandardEntityNotFoundDelegate();

	@Override
	public void handleEntityNotFound(String entityName, Serializable id) {
		throw new ObjectNotFoundException( id, entityName );
	}
}
