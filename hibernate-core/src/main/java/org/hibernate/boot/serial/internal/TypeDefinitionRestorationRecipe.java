/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;

/// Declarative archive form of a named [TypeDefinition].
///
/// The implementor is deliberately stored by name so restoration resolves the
/// extension class from the consumer environment.
///
/// @since 9.0
/// @author Steve Ebersole
record TypeDefinitionRestorationRecipe(
		String name,
		String typeImplementorClassName,
		String[] registrationKeys,
		Map<String, String> parameters) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	TypeDefinitionRestorationRecipe {
		registrationKeys = registrationKeys == null ? null : registrationKeys.clone();
		parameters = parameters == null
				? null
				: Collections.unmodifiableMap( new HashMap<>( parameters ) );
	}

	@Override
	public String[] registrationKeys() {
		return registrationKeys == null ? null : registrationKeys.clone();
	}

	static TypeDefinitionRestorationRecipe from(TypeDefinition definition) {
		return new TypeDefinitionRestorationRecipe(
				definition.getName(),
				definition.getTypeImplementorClass().getName(),
				definition.getRegistrationKeys(),
				definition.getParameters()
		);
	}

	TypeDefinition resolve(ClassLoaderService classLoaderService) {
		return resolve( classLoaderService::classForTypeName );
	}

	TypeDefinition resolve(Function<String, Class<?>> classResolver) {
		final Class<?> implementorClass;
		try {
			implementorClass = classResolver.apply( typeImplementorClassName );
		}
		catch (RuntimeException e) {
			throw new IllegalStateException(
					"Could not resolve implementor class '" + typeImplementorClassName
							+ "' for archived type definition '" + name + "'",
					e
			);
		}
		return new TypeDefinition( name, implementorClass, registrationKeys, parameters );
	}
}
