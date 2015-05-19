/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.buildtime.spi;

/**
 * Provides an abstraction for how instrumentation does logging because it is usually run in environments (Ant/Maven)
 * with their own logging infrastructure.  This abstraction allows proper bridging.
 *
 * @author Steve Ebersole
 */
public interface Logger {
	/**
	 * Log a message with TRACE semantics.
	 *
	 * @param message The message to log.
	 */
	public void trace(String message);

	/**
	 * Log a message with DEBUG semantics.
	 *
	 * @param message The message to log.
	 */
	public void debug(String message);

	/**
	 * Log a message with INFO semantics.
	 *
	 * @param message The message to log.
	 */
	public void info(String message);

	/**
	 * Log a message with WARN semantics.
	 *
	 * @param message The message to log.
	 */
	public void warn(String message);

	/**
	 * Log a message with ERROR semantics.
	 *
	 * @param message The message to log.
	 */
	public void error(String message);
}
