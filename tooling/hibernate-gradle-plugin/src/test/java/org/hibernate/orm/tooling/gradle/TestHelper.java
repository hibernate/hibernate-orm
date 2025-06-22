/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.tooling.gradle;

import org.hibernate.engine.spi.Managed;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
public class TestHelper {

	public static void verifyEnhanced(ClassLoader classLoader, String className) throws Exception {
		final Class<?> loadedClass = classLoader.loadClass( className );
		assertThat( Managed.class ).isAssignableFrom( loadedClass );
	}
}
