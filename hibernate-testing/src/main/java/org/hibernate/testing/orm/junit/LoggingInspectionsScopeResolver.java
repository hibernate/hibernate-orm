/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * ParameterResolver implementation for resolving
 * {@link LoggingInspectionsScope} ParameterResolver
 */
public class LoggingInspectionsScopeResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) {
		return LoggingInspectionsScope.class.isAssignableFrom(
				parameterContext.getParameter().getType()
		);
	}

	@Override
	public LoggingInspectionsScope resolveParameter(
			ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		return LoggingInspectionsExtension.resolveLoggingInspectionScope(
				extensionContext.getRequiredTestInstance(),
				extensionContext
		);
	}
}
