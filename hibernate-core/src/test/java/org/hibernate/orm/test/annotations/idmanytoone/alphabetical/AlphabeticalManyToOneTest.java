/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;

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
