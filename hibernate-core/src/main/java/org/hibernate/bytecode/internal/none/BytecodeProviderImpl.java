/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.none;

import jakarta.annotation.Nonnull;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;

import jakarta.annotation.Nullable;

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

	@Nonnull
	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new NoProxyFactoryFactory();
	}

	@Override
	public @Nullable Enhancer getEnhancer(@Nonnull EnhancementContext enhancementContext) {
		return null;
	}
}
