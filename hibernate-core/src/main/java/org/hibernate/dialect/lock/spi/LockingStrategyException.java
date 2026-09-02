/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.lock.spi;

import org.hibernate.HibernateException;
import org.hibernate.SPI;

/// An error applying a [LockingStrategy] to an entity.
///
/// @since 8.0
/// @author Steve Ebersole
@SPI({ SPI.Role.USE, SPI.Role.IMPLEMENT })
public abstract class LockingStrategyException extends HibernateException {
	private final Object entity;

	/// Creates a locking failure for the given entity and message.
	@SPI(SPI.Role.IMPLEMENT)
	public LockingStrategyException(Object entity, String message) {
		super( message );
		this.entity = entity;
	}

	/// Creates a locking failure for the given entity, message, and underlying
	/// cause.
	@SPI(SPI.Role.IMPLEMENT)
	public LockingStrategyException(Object entity, String message, Throwable cause) {
		super( message, cause );
		this.entity = entity;
	}

	/// The entity for which locking failed.
	public Object getEntity() {
		return entity;
	}
}
