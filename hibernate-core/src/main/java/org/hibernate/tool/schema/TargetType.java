/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema;

/**
 * Describes the allowable targets (SPI wise) for schema management actions.
 * <p>
 * Under the covers corresponds to provider-specific implementations of
 * {@link org.hibernate.tool.schema.spi.GenerationTarget}.
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
