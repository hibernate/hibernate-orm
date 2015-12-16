package org.hibernate.property.access.spi;

import java.lang.reflect.Field;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class GetterFieldImplTest {

	@Test
	public void testGet() throws Exception {
		Target target = new Target();

		assertEquals( true, getter( "active" ).get( target ) );
		assertEquals( (byte) 2, getter( "children" ).get( target ) );
		assertEquals( 'M', getter( "gender" ).get( target ) );
		assertEquals( Integer.MAX_VALUE, getter( "code" ).get( target ) );
		assertEquals( Long.MAX_VALUE, getter( "id" ).get( target ) );
		assertEquals( (short) 34, getter( "age" ).get( target ) );
		assertEquals( "John Doe", getter( "name" ).get( target ) );
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