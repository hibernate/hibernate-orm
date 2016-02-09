/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema;

/**
 * Describes the allowable targets (SPI wise) for schema management actions.
 * <p/>
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
	STDOUT;
}
