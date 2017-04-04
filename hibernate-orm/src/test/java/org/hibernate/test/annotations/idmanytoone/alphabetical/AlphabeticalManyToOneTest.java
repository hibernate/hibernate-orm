/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.idmanytoone.alphabetical;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class AlphabeticalManyToOneTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testAlphabeticalTest() throws Exception {
		//test through deployment
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Acces.class,
				Droitacces.class,
				Benefserv.class,
				Service.class
		};
	}
}
