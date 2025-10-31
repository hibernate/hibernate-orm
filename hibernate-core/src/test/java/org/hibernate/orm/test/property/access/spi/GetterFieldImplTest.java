/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property.access.spi;

import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterFieldImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
public class GetterFieldImplTest {

	@Test
	public void testGet() {
		Target target = new Target();

		assertThat( getter( "active" ).get( target ) ).isEqualTo( true );
		assertThat( getter( "children" ).get( target ) ).isEqualTo( (byte) 2 );
		assertThat( getter( "gender" ).get( target ) ).isEqualTo( 'M' );
		assertThat( getter( "code" ).get( target ) ).isEqualTo( Integer.MAX_VALUE );
		assertThat( getter( "id" ).get( target ) ).isEqualTo( Long.MAX_VALUE );
		assertThat( getter( "age" ).get( target ) ).isEqualTo( (short) 34 );
		assertThat( getter( "name" ).get( target ) ).isEqualTo( "John Doe" );
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
