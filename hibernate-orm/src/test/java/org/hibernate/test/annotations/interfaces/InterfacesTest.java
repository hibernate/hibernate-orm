/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.interfaces;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class InterfacesTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testInterface() {
		// test via SessionFactory building
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { ContactImpl.class, UserImpl.class };
	}
}
