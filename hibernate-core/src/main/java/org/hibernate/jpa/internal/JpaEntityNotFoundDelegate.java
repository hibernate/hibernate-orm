/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal;

import jakarta.persistence.EntityNotFoundException;
import org.hibernate.ObjectNotFoundException;
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
