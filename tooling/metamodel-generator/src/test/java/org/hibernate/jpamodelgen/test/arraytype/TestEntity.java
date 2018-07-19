/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.arraytype;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.persistence.Entity;
import javax.persistence.Id;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Chris Cranford
 */
@Entity
public class TestEntity {
	@Id
	private Integer id;

	// Primitive array test
	@MySize
	private byte[] primitiveAnnotatedArray;
	private byte[] primitiveArray;

	// Primitive non-array test
	@MySize
	private byte primitiveAnnotated;
	private byte primitive;

	// Non-primitive array test
	@MySize
	private Byte[] nonPrimitiveAnnotatedArray;
	private Byte[] nonPrimitiveArray;

	// Non-primitive non-array test
	@MySize
	private Byte nonPrimitiveAnnotated;
	private Byte nonPrimitive;

	// Custom array test
	@MySize
	private CustomType[] customAnnotatedArray;
	private CustomType[] customArray;

	@Target({FIELD, TYPE_USE})
	@Retention(RUNTIME)
	public @interface MySize {

	}

	// some custom type
	public static class CustomType {
		private Integer id;
	}
}
