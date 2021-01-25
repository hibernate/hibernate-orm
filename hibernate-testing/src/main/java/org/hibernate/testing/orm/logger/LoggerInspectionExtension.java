/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.orm.logger;

import org.hibernate.testing.logger.LogInspectionHelper;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public final class LoggerInspectionExtension implements AfterEachCallback, BeforeEachCallback, ParameterResolver {

	private Inspector inspector;

	public LoggerInspectionExtension() {
		inspector = new Inspector();
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		// do nothing
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		LogInspectionHelper.clearAllListeners( inspector.getLog() );
	}

	@Override
	public boolean supportsParameter(
			ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return parameterContext.isAnnotated( LogInspector.class );
	}

	@Override
	public Object resolveParameter(
			ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return inspector;
	}
}
