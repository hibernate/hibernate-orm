/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.none;

import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.property.access.spi.PropertyAccess;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This BytecodeProvider represents the "no-op" enhancer; mostly useful
 * as an optimisation when not needing any byte code optimisation applied,
 * for example when the entities have been enhanced at compile time.
 * Choosing this BytecodeProvider allows to exclude the bytecode enhancement
 * libraries from the runtime classpath.
 *
 * @since 5.4
 */
public final class BytecodeProviderImpl implements BytecodeProvider {

	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new NoProxyFactoryFactory();
	}

	@Override
	public ReflectionOptimizer getReflectionOptimizer(
			Class clazz,
			String[] getterNames,
			String[] setterNames,
			Class[] types) {
		throw new HibernateException( "Using the ReflectionOptimizer is not possible when the configured BytecodeProvider is 'none'. Use a different BytecodeProvider" );
	}

	@Override
	public @Nullable ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap) {
		return null;
	}

	@Override
	public @Nullable Enhancer getEnhancer(EnhancementContext enhancementContext) {
		return null;
	}
}
