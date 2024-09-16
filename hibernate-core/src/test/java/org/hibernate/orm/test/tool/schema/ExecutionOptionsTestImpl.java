/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tool.schema;

import java.util.Collections;
import java.util.Map;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;

/**
 * @author Steve Ebersole
 */
public class ExecutionOptionsTestImpl implements ExecutionOptions, ExceptionHandler {
	/**
	 * Singleton access for standard cases.  Returns an empty map of configuration values,
	 * true that namespaces should be managed and it always re-throws command exceptions
	 */
	public static final ExecutionOptionsTestImpl INSTANCE = new ExecutionOptionsTestImpl();

	@Override
	public Map getConfigurationValues() {
		return Collections.emptyMap();
	}

	@Override
	public boolean shouldManageNamespaces() {
		return true;
	}

	@Override
	public ExceptionHandler getExceptionHandler() {
		return this;
	}

	@Override
	public SchemaFilter getSchemaFilter() {
		return SchemaFilter.ALL;
	}

	@Override
	public void handleException(CommandAcceptanceException exception) {
		throw exception;
	}
}
