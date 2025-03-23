/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.Locale;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class ExceptionHandlerHaltImpl implements ExceptionHandler {
	/**
	 * Singleton access
	 */
	public static final ExceptionHandlerHaltImpl INSTANCE = new ExceptionHandlerHaltImpl();

	@Override
	public void handleException(CommandAcceptanceException exception) {
		throw new SchemaManagementException(
				String.format(
						Locale.ROOT,
						"Halting on error : %s",
						exception.getMessage()
				),
				exception
		);
	}
}
