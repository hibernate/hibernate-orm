/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;

import org.hibernate.Incubating;

/**
 * Parameter object representing options for schema management tool execution
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ExecutionOptions {
	Map<String,Object> getConfigurationValues();

	boolean shouldManageNamespaces();

	ExceptionHandler getExceptionHandler();
}
