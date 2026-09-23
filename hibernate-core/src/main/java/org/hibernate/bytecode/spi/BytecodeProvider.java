/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.Service;



/**
 * Contract for providers of bytecode services to Hibernate.
 * <p>
 * Bytecode requirements break down into the following areas<ol>
 *     <li>proxy generation (both for runtime-lazy-loading and basic proxy generation) {@link #getProxyFactoryFactory()}</li>
 * </ol>
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface BytecodeProvider extends Service {
	/**
	 * Retrieve the specific factory for this provider capable of
	 * generating run-time proxies for lazy-loading purposes.
	 *
	 * @return The provider specific factory.
	 */
	@Nonnull
	ProxyFactoryFactory getProxyFactoryFactory();

	/// Creates a privately owned session for a stable model and bytecode environment.
	@Nonnull EnhancementSession createEnhancementSession(
			EnhancementModel model,
			EnhancementEnvironment environment);

	/**
	 * Some BytecodeProvider implementations will have classloader specific caching.
	 * These caches are useful at runtime but need to be reset at least on SessionFactory shutdown
	 * to prevent leaking the deployment classloader.
	 * Since the BytecodeProvider is static these caches are potentially shared across multiple
	 * deployments; in this case we'll clear all caches which might show as a small, temporary
	 * performance degradation on the SessionFactory instances which haven't been closed.
	 * This limitation will be removed in the future, when these providers will no longer be static.
	 */
	default void resetCaches() {}

}
