/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.transaction.jta.platform.spi;

import org.hibernate.service.JavaServiceLoadable;

/**
 * A {@link java.util.ServiceLoader}-style provider of {@link JtaPlatform}
 * instances. Used when an explicit {@code JtaPlatform} is not provided.
 *
 * @see JtaPlatform
 * @see JtaPlatformResolver
 *
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface JtaPlatformProvider {
	/**
	 * Retrieve the JtaPlatform provided by this environment.
	 *
	 * @return The provided JtaPlatform
	 */
	JtaPlatform getProvidedJtaPlatform();
}
