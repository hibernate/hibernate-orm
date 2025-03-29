/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property.access.spi;

import java.lang.reflect.Field;

import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Vlad Mihalcea
 */
public class GetterFieldImplTest {

	@Test
	public void testGet() throws Exception {
		Target target = new Target();

		Assert.assertEquals( true, getter( "active" ).get( target ) );
		Assert.assertEquals( (byte) 2, getter( "children" ).get( target ) );
		Assert.assertEquals( 'M', getter( "gender" ).get( target ) );
		Assert.assertEquals( Integer.MAX_VALUE, getter( "code" ).get( target ) );
		Assert.assertEquals( Long.MAX_VALUE, getter( "id" ).get( target ) );
		Assert.assertEquals( (short) 34, getter( "age" ).get( target ) );
		Assert.assertEquals( "John Doe", getter( "name" ).get( target ) );
	}

	private static class Target {

		private boolean active = true;

		private byte children = 2;

		private char gender = 'M';

		private int code = Integer.MAX_VALUE;

		private long id = Long.MAX_VALUE;

		private short age = 34;

		private String name = "John Doe";
	}

	private Getter getter(String property) {
		try {
			Field field = Target.class.getDeclaredField( property );
			field.setAccessible( true );
			return new GetterFieldImpl( Target.class, property, field );
		}
		catch (NoSuchFieldException e) {
			throw new IllegalArgumentException( e );
		}
	}
}
