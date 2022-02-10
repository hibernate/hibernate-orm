/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;

/**
 * @author Steve Ebersole
 */
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
			if ( parameterContext.getParameter().getType().isAssignableFrom( supportedType ) ) {
				return true;
			}
		}

		return false;
	}
}
