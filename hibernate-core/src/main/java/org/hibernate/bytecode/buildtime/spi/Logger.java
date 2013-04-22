/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
