/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.processing;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;

/**
 * A generic {@linkplain Dialect dialect} for ANSI-like SQL.
 * Used by default in the HQL Query Validator.
 *
 * @author Gavin King
 *
 * @see CheckHQL#dialect
 */
public class GenericDialect extends Dialect {
	public GenericDialect() {
		super( (DatabaseVersion) null );
	}

	@Override
	public LockingSupport getLockingSupport() {
		return NoLockingSupport.NO_LOCKING_SUPPORT;
	}
}
