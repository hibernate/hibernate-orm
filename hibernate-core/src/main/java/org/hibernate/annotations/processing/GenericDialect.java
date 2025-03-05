/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.processing;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

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
}
