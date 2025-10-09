/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cdi.testsupport;

import org.hibernate.testing.orm.junit.JUnitHelper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Steve Ebersole
 */
public class CdiContainerParameterResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return JUnitHelper.supportsParameterInjection( parameterContext, CdiContainerScope.class );
	}

	@Override
	public @Nullable Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return CdiContainerExtension.findCdiContainerScope( extensionContext.getRequiredTestInstance(), extensionContext );
	}
}
