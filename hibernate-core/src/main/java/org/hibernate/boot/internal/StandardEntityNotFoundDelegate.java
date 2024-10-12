/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.internal;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.proxy.EntityNotFoundDelegate;

/**
 * Standard non-JPA implementation of {@link EntityNotFoundDelegate}, throwing the
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
	public void handleEntityNotFound(String entityName, Object id) {
		throw new ObjectNotFoundException( entityName, id );
	}
}
