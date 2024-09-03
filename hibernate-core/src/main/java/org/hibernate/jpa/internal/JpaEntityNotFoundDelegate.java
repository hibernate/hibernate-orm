/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.jpa.internal;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.proxy.EntityNotFoundDelegate;

import java.io.Serializable;

public class JpaEntityNotFoundDelegate implements EntityNotFoundDelegate, Serializable {
	/**
	 * Singleton access
	 */
	public static final JpaEntityNotFoundDelegate INSTANCE = new JpaEntityNotFoundDelegate();

	public void handleEntityNotFound(String entityName, Object identifier) {
		final ObjectNotFoundException exception = new ObjectNotFoundException( entityName, identifier );
		throw new EntityNotFoundException( exception.getMessage(), exception );
	}
}
