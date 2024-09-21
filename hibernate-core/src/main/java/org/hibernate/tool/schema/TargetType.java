/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema;

/**
 * Describes the allowable targets (SPI wise) for schema management actions.
 * <p>
 * Under the covers corresponds to provider-specific implementations of
 * {@link org.hibernate.tool.schema.internal.exec.GenerationTarget}
 *
 * @author Steve Ebersole
 */
public enum TargetType {
	/**
	 * Export to the database.
	 */
	DATABASE,
	/**
	 * Write to a script file.
	 */
	SCRIPT,
	/**
	 * Write to {@link System#out}
	 */
	STDOUT
}
