/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.optional.javassist;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertNull;

public class OptionalJavassistDependencyTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void shouldNotBeOnTheClasspath() throws Exception {
		thrown.expect( ClassNotFoundException.class );

		Class<?> aClass = getClass().getClassLoader().loadClass( "javassit.Classpath" );
	}
}
