/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class ExceptionHandlerLoggedImpl implements ExceptionHandler {
	private static final Logger LOG = Logger.getLogger( ExceptionHandlerLoggedImpl.class );

	/**
	 * Singleton access
	 */
	public static final ExceptionHandlerLoggedImpl INSTANCE = new ExceptionHandlerLoggedImpl();

	@Override
	public void handleException(CommandAcceptanceException exception) {
		LOG.warnf(
				exception,
				"GenerationTarget encountered exception accepting command : %s",
				exception.getMessage()
		);
	}
}
