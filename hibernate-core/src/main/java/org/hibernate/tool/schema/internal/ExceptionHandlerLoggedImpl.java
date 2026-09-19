/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;

import static org.hibernate.tool.schema.internal.SchemaManagementLogging.SCHEMA_LOGGER;

/**
 * @author Steve Ebersole
 */
public class ExceptionHandlerLoggedImpl implements ExceptionHandler {
	/**
	 * Singleton access
	 */
	public static final ExceptionHandlerLoggedImpl INSTANCE = new ExceptionHandlerLoggedImpl();

	@Override
	public void handleException(CommandAcceptanceException exception) {
		SCHEMA_LOGGER.generationTargetEncounteredException( exception.getMessage(), exception );
	}
}
