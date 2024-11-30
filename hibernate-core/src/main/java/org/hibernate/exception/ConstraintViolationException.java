/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.exception;
import java.sql.SQLException;

import org.hibernate.JDBCException;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link JDBCException} indicating that the requested DML operation
 * resulted in violation of a defined integrity constraint.
 *
 * @author Steve Ebersole
 */
public class ConstraintViolationException extends JDBCException {

	private final ConstraintKind kind;
	private final @Nullable String constraintName;

	public ConstraintViolationException(String message, SQLException root, @Nullable String constraintName) {
		this( message, root, ConstraintKind.OTHER, constraintName );
	}

	public ConstraintViolationException(String message, SQLException root, String sql, @Nullable String constraintName) {
		this( message, root, sql, ConstraintKind.OTHER, constraintName );
	}

	public ConstraintViolationException(String message, SQLException root, ConstraintKind kind, @Nullable String constraintName) {
		super( message, root );
		this.kind = kind;
		this.constraintName = constraintName;
	}

	public ConstraintViolationException(String message, SQLException root, String sql, ConstraintKind kind, @Nullable String constraintName) {
		super( message, root, sql );
		this.kind = kind;
		this.constraintName = constraintName;
	}

	/**
	 * Returns the name of the violated constraint, if known.
	 *
	 * @return The name of the violated constraint, or null if not known.
	 */
	public @Nullable String getConstraintName() {
		return constraintName;
	}

	/**
	 * Returns the kind of constraint that was violated.
	 */
	public ConstraintKind getKind() {
		return kind;
	}

	public enum ConstraintKind {
		UNIQUE,
		OTHER
	}
}
