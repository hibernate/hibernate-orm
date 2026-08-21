/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.test.utils;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.internal.ExceptionHandlerHaltImpl;
import org.hibernate.tool.schema.spi.ContributableMatcher;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;

import java.util.HashMap;
import java.util.Map;

public class SchemaUtil {

	public static void validateSchema(Metadata metadata, ServiceRegistry serviceRegistry) {
		Map<String, Object> config =
				new HashMap<>( serviceRegistry.requireService( ConfigurationService.class ).getSettings() );

		final SchemaManagementTool tool = serviceRegistry.requireService( SchemaManagementTool.class );

		final ExecutionOptions executionOptions = SchemaManagementToolCoordinator.buildExecutionOptions(
				config,
				ExceptionHandlerHaltImpl.INSTANCE
		);

		tool.getSchemaValidator( config ).doValidation( metadata, executionOptions, ContributableMatcher.ALL );
	}

}
