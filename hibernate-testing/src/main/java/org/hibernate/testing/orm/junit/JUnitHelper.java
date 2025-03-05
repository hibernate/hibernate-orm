/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.Internal;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;

/**
 * @author Steve Ebersole
 */
@Internal
public class JUnitHelper {
	public static ExtensionContext.Store locateExtensionStore(
			Class<? extends Extension> extensionClass,
			ExtensionContext context,
			Object scopeObject) {
		return context.getStore( create( extensionClass.getName(), scopeObject ) );
	}

	public static ExtensionContext.Store locateExtensionStore(
			ExtensionContext context,
			Object... scopeRefs) {
		return context.getStore( create( scopeRefs ) );
	}

	private JUnitHelper() {
	}

	public static boolean supportsParameterInjection(ParameterContext parameterContext, Class<?>... supportedTypes) {
		for ( Class<?> supportedType : supportedTypes ) {
			if ( supportedType.isAssignableFrom( parameterContext.getParameter().getType() ) ) {
				return true;
			}
		}

		return false;
	}
}
