/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/// ParameterResolver for [MessageKeyWatcher]
///
/// @see MessageKeyInspectionExtension
///
/// @author Steve Ebersole
public class MessageKeyWatcherResolver implements ParameterResolver {
	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return MessageKeyWatcher.class.isAssignableFrom( parameterContext.getParameter().getType() );
	}

	@Override
	public MessageKeyWatcher resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
		return MessageKeyInspectionExtension.getWatcher( extensionContext );
	}
}
