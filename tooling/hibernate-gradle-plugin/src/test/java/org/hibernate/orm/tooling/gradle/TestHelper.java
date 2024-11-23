/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
