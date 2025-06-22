/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.service.JavaServiceLoadable;

/**
 * An extension point for integrators that wish to hook into the process of how a SessionFactory
 * is built. Intended as a "discoverable service" ({@link java.util.ServiceLoader}). There can
 * be at most one implementation discovered that returns a non-null {@link SessionFactoryBuilder}.
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface SessionFactoryBuilderFactory {
	/**
	 * The contract method. Return the {@link SessionFactoryBuilder}. May return {@code null}
	 *
	 * @param metadata The metadata from which we will be building a {@code SessionFactory}.
	 * @param defaultBuilder The default {@code SessionFactoryBuilder} instance.
	 *                       If the {@code SessionFactoryBuilder} being built here needs to use
	 *                       this passed {@code SessionFactoryBuilder} instance, it is the
	 *                       responsibility of the built {@code SessionFactoryBuilder} impl
	 *                       to delegate configuration calls to the passed default impl.
	 *
	 * @return The {@code SessionFactoryBuilder}, or {@code null}
	 */
	SessionFactoryBuilder getSessionFactoryBuilder(MetadataImplementor metadata, SessionFactoryBuilderImplementor defaultBuilder);
}
