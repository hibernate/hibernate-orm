/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.property.access.spi;

import org.hibernate.PropertyAccessException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterMethodImpl;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class SetterMethodImplTest {

	@Test
	public void set() throws Exception {
		Target target = new Target();

		setter( Target.class, "active", boolean.class ).set( target, true );
		assertThat( target.active ).isEqualTo( true );
		setter( Target.class, "children", byte.class ).set( target, (byte) 2 );
		assertThat( target.children ).isEqualTo( (byte) 2 );
		setter( Target.class, "gender", char.class ).set( target, 'M' );
		assertThat( target.gender ).isEqualTo( 'M' );
		setter( Target.class, "code", int.class ).set( target, Integer.MAX_VALUE );
		assertThat( target.code ).isEqualTo( Integer.MAX_VALUE );
		setter( Target.class, "id", long.class ).set( target, Long.MAX_VALUE );
		assertThat( target.id ).isEqualTo( Long.MAX_VALUE );
		setter( Target.class, "age", short.class ).set( target, (short) 34 );
		assertThat( target.age ).isEqualTo( (short) 34 );
		setter( Target.class, "name", String.class ).set( target, "John Doe" );
		assertThat( target.name ).isEqualTo( "John Doe" );
	}

	private static class Target {

		private boolean active;

		private byte children;

		private char gender;

		private int code;

		private long id;

		private short age;

		private String name;

		private void setActive(boolean active) {
			this.active = active;
		}

		private void setChildren(byte children) {
			this.children = children;
		}

		private void setGender(char gender) {
			this.gender = gender;
		}

		private void setCode(int code) {
			this.code = code;
		}

		private void setId(long id) {
			this.id = id;
		}

		private void setAge(short age) {
			this.age = age;
		}

		private void setName(String name) {
			this.name = name;
		}
	}

	@Test
	public void setThrowing() {
		TargetThrowingExceptions target = new TargetThrowingExceptions();

		Setter runtimeException = setter( TargetThrowingExceptions.class, "runtimeException", String.class );
		assertThatThrownBy( () -> runtimeException.set( target, "foo" ) )
				.isInstanceOf( PropertyAccessException.class )
				.hasMessage(
						"Exception occurred inside: '" + TargetThrowingExceptions.class.getName() + ".runtimeException' (setter)" )
				.cause() // Not the root cause, the *direct* cause! We don't want extra wrapping.
				.isExactlyInstanceOf( RuntimeException.class );

		Setter checkedException = setter( TargetThrowingExceptions.class, "checkedException", String.class );
		assertThatThrownBy( () -> checkedException.set( target, "foo" ) )
				.isInstanceOf( PropertyAccessException.class )
				.hasMessage(
						"Exception occurred inside: '" + TargetThrowingExceptions.class.getName() + ".checkedException' (setter)" )
				.cause() // Not the root cause, the *direct* cause! We don't want extra wrapping.
				.isExactlyInstanceOf( Exception.class );

		Setter error = setter( TargetThrowingExceptions.class, "error", String.class );
		assertThatThrownBy( () -> error.set( target, "foo" ) )
				.isExactlyInstanceOf( Error.class ); // We don't want *any* wrapping!
	}

	private static class TargetThrowingExceptions {
		private void setRuntimeException(String ignored) {
			throw new RuntimeException();
		}

		private void setCheckedException(String ignored) throws Exception {
			throw new Exception();
		}

		private void setError(String ignored) {
			throw new Error();
		}
	}

	private Setter setter(Class<?> clazz, String property, Class<?> type) {
		final Method setterMethod = ReflectHelper.findSetterMethod( clazz, property, type );
		return new SetterMethodImpl( clazz, property, setterMethod );
	}
}
