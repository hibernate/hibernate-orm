/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

/**
 * Contract for how CommandAcceptanceException errors should be handled (logged, ignored, etc).
 *
 * @author Steve Ebersole
 */
public interface ExceptionHandler {
	/**
	 * Handle the CommandAcceptanceException error
	 *
	 * @param exception The CommandAcceptanceException to handle
	 */
	void handleException(CommandAcceptanceException exception);
}
