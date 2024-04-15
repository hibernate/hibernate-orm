/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
