/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property.access.spi;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;

import org.hibernate.PropertyAccessException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterMethodImpl;

import org.junit.Assert;
import org.junit.Test;


public class GetterMethodImplTest {

	@Test
	public void get() throws Exception {
		Target target = new Target();

		Assert.assertEquals( true, getter( Target.class, "active" ).get( target ) );
		Assert.assertEquals( (byte) 2, getter( Target.class, "children" ).get( target ) );
		Assert.assertEquals( 'M', getter( Target.class, "gender" ).get( target ) );
		Assert.assertEquals( Integer.MAX_VALUE, getter( Target.class, "code" ).get( target ) );
		Assert.assertEquals( Long.MAX_VALUE, getter( Target.class, "id" ).get( target ) );
		Assert.assertEquals( (short) 34, getter( Target.class, "age" ).get( target ) );
		Assert.assertEquals( "John Doe", getter( Target.class, "name" ).get( target ) );
	}

	private static class Target {

		private boolean active = true;

		private byte children = 2;

		private char gender = 'M';

		private int code = Integer.MAX_VALUE;

		private long id = Long.MAX_VALUE;

		private short age = 34;

		private String name = "John Doe";

		private boolean isActive() {
			return active;
		}

		private byte getChildren() {
			return children;
		}

		private char getGender() {
			return gender;
		}

		private int getCode() {
			return code;
		}

		private long getId() {
			return id;
		}

		private short getAge() {
			return age;
		}

		private String getName() {
			return name;
		}
	}

	@Test
	public void getThrowing() {
		TargetThrowingExceptions target = new TargetThrowingExceptions();

		Getter runtimeException = getter( TargetThrowingExceptions.class, "runtimeException" );
		assertThatThrownBy( () -> runtimeException.get( target ) )
				.isInstanceOf( PropertyAccessException.class )
				.hasMessage( "Exception occurred inside: '" + TargetThrowingExceptions.class.getName() +".runtimeException' (getter)" )
				.getCause() // Not the root cause, the *direct* cause! We don't want extra wrapping.
				.isExactlyInstanceOf( RuntimeException.class );

		Getter checkedException = getter( TargetThrowingExceptions.class, "checkedException" );
		assertThatThrownBy( () -> checkedException.get( target ) )
				.isInstanceOf( PropertyAccessException.class )
				.hasMessage( "Exception occurred inside: '" + TargetThrowingExceptions.class.getName() +".checkedException' (getter)" )
				.getCause() // Not the root cause, the *direct* cause! We don't want extra wrapping.
				.isExactlyInstanceOf( Exception.class );

		Getter error = getter( TargetThrowingExceptions.class, "error" );
		assertThatThrownBy( () -> error.get( target ) )
				.isExactlyInstanceOf( Error.class ); // We don't want *any* wrapping!
	}

	private static class TargetThrowingExceptions {
		private String getRuntimeException() {
			throw new RuntimeException();
		}

		private String getCheckedException() throws Exception {
			throw new Exception();
		}

		private String getError() {
			throw new Error();
		}
	}

	private Getter getter(Class<?> clazz, String property) {
		final Method getterMethod = ReflectHelper.findGetterMethod( clazz, property );
		return new GetterMethodImpl( clazz, property, getterMethod );
	}
}
