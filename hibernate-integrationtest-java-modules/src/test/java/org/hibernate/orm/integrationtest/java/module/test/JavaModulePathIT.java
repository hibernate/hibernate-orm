/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test;


import java.util.Arrays;

import org.hibernate.Session;
import org.hibernate.orm.integrationtest.java.module.test.service.AuthorService;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JavaModulePathIT {

	/*
	 * Test that the service successfully uses Hibernate ORM in the module path.
	 * We don't really care about the features themselves,
	 * but the easiest way to check this is to just use Hibernate ORM features and see if it works.
	 */
	@Test
	public void core() {
		checkIsInModulePath( Object.class );
		checkIsInModulePath( AuthorService.class );
		checkIsInModulePath( Session.class );

		AuthorService service = new AuthorService();
		service.add( "foo", 7 );
		service.add( "bar", 42 );
		service.add( "foo bar", 777 );

		service.update( "foo", 8 );

		assertEquals( (Integer) 8, service.getFavoriteNumber( "foo" ) );
		assertEquals( (Integer) 42, service.getFavoriteNumber( "bar" ) );
		assertEquals( (Integer) 777, service.getFavoriteNumber( "foo bar" ) );
	}

	/*
	 * Test that the service successfully uses an extension of Hibernate ORM in the module path.
	 * We don't really care about the features themselves,
	 * but the easiest way to check this is to just use Envers features and see if it works.
	 */
	@Test
	public void integrator() {
		checkIsInModulePath( Object.class );
		checkIsInModulePath( AuthorService.class );
		checkIsInModulePath( Session.class );

		AuthorService service = new AuthorService();
		service.add( "foo", 7 );
		service.add( "bar", 42 );
		service.add( "foo bar", 777 );

		service.update( "foo", 8 );
	}

	private void checkIsInModulePath(Class<?> clazz) {
		Assert.assertTrue(
				clazz + " should be part of a named module - there is a problem in test setup",
				clazz.getModule().isNamed()
		);
	}
}
