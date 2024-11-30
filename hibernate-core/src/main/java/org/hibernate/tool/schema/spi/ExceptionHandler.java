/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
