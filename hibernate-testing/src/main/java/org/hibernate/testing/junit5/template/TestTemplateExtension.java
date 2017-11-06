/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit5.template;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * @author Andrea Boriero
 */
public abstract class TestTemplateExtension
		implements TestTemplateInvocationContextProvider {

	public class CustomTestTemplateInvocationContext
			implements TestTemplateInvocationContext {
		private final String displayName;
		private final ParameterResolver parameterResolver;

		public CustomTestTemplateInvocationContext(TestParameter parameter, Class<? extends TestScope> parameterClass) {
			this.displayName = parameter.getValue().toString();
			this.parameterResolver = new TestScopeParameterResolver( parameter, parameterClass );
		}

		@Override
		public String getDisplayName(int invocationIndex) {
			return displayName;
		}

		@Override
		public List<Extension> getAdditionalExtensions() {
			return Collections.singletonList( parameterResolver );
		}
	}

	public class TestScopeParameterResolver implements ParameterResolver {
		private final Class<?> parameterClass;
		private final TestParameter parameter;
		private TestScope testScope;

		public TestScopeParameterResolver(TestParameter parameter, Class<?> parameterClass) {
			this.parameter = parameter;
			this.parameterClass = parameterClass;
		}

		@Override
		public boolean supportsParameter(
				ParameterContext parameterContext,
				ExtensionContext extensionContext)
				throws ParameterResolutionException {
			return parameterContext.getParameter().getType().equals( parameterClass );
		}

		@Override
		public TestScope resolveParameter(
				ParameterContext parameterContext,
				ExtensionContext extensionContext) {
			final Object testInstance = extensionContext.getRequiredTestInstance();
			if ( !TestScopeProducer.class.isInstance( testInstance ) ) {
				throw new RuntimeException( "Test instance does not implement TestScopeProducer" );
			}
			if ( testScope == null ) {
				testScope = ( (TestScopeProducer) testInstance ).produceTestScope( parameter );
			}
			return testScope;
		}
	}
}
