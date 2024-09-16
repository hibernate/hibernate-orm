/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.spi;

import java.util.Map;

/**
 * Provide a way to customize the {@link org.hibernate.type.Type} instantiation process.
 * <p>
 * If a custom {@link org.hibernate.type.Type} defines a constructor which takes the
 * {@link TypeBootstrapContext} argument, Hibernate will use this instead of the
 * default constructor.
 *
 * @author Vlad Mihalcea
 *
 * @since 5.4
 */
public interface TypeBootstrapContext {
	@SuppressWarnings("unused")
	Map<String, Object> getConfigurationSettings();
}
