/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;

/**
 * @author Steve Ebersole
 */
public class ExceptionHandlerCollectingImpl implements ExceptionHandler {
	private final List<CommandAcceptanceException> exceptions = new ArrayList<>();

	public ExceptionHandlerCollectingImpl() {
	}

	@Override
	public void handleException(CommandAcceptanceException exception) {
		exceptions.add( exception );
	}

	public List<CommandAcceptanceException> getExceptions() {
		return exceptions;
	}
}
