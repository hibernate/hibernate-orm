/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.tool.schema;

import java.util.Collections;
import java.util.Map;

import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.hibernate.tool.schema.spi.ExceptionHandler;
import org.hibernate.tool.schema.spi.ExecutionOptions;

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
	public void handleException(CommandAcceptanceException exception) {
		throw exception;
	}
}
