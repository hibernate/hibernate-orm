/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the UDT (user defined type) name for the annotated embeddable
 * type or embedded attribute.
 * <p>
 * This annotation may be applied to an embeddable class:
 * <pre>
 * {@code @Embeddable}
 * {@code @Struct(name = "CUST")}
 * public class Customer { ... }
 * </pre>
 * <p>
 * Alternatively, it may be applied to an embedded attribute:
 * <pre>
 * public class Order {
 *     {@code @Embedded}
 *     {@code @Struct(name = "CUST")}
 *     private Customer customer;
 * }
 * </pre>
 *
 * @since 6.2
 */
@Incubating
@Target({TYPE, FIELD, METHOD})
@Retention( RUNTIME )
public @interface Struct {
	/**
	 * The name of the UDT (user defined type).
	 */
	String name();

	/** (Optional) The catalog of the UDT.
	 * <p> Defaults to the default catalog.
	 */
	String catalog() default "";

	/** (Optional) The schema of the UDT.
	 * <p> Defaults to the default schema for user.
	 */
	String schema() default "";

	/**
	 * The ordered set of attributes of the UDT, as they appear physically in the DDL.
	 * It is important to specify the attributes in the same order for JDBC interactions to work correctly.
	 * If the annotated type is a record, the order of record components is used as the default order.
	 * If no default order can be inferred, attributes are assumed to be in alphabetical order.
	 */
	String[] attributes() default {};
}
