/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bootstrap.binding.naming;

import org.hibernate.boot.model.naming.ImplicitIndexColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNameSource;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Dmytro Bondar
 */
public class ImplicitIndexColumnNameSourceTest {

	@Test
	@JiraKey(value = "HHH-10810")
	public void testExtensionImplicitNameSource() {
		assertTrue( ImplicitNameSource.class.isAssignableFrom( ImplicitIndexColumnNameSource.class ) );
	}

}
