/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to override the collection table name for a collection
 * that is nested within an embeddable class.
 *
 * <p>This annotation allows overriding the collection table name
 * for collections that are defined within an embeddable class.
 *
 * <p>Example:
 * <pre>
 * &#064;Embeddable
 * public class Address {
 *     &#064;ElementCollection
 *     &#064;CollectionTable(name = "default_phones")
 *     List&lt;Phone&gt; phones;
 * }
 *
 * &#064;Entity
 * public class Person {
 *     &#064;Embedded
 *     &#064;CollectionTableOverride(
 *         name = "phones",
 *         table = "person_phones"
 *     )
 *     Address address;
 * }
 * </pre>
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
public @interface CollectionTableOverride {
	/**
	 * The path to the collection property within the embeddable.
	 * For example, if the embeddable has a field "phones", the name would be "phones".
	 * For nested embeddables, use dot notation like "address.phones".
	 */
	String name();

	/**
	 * The name of the collection table to use instead of the default.
	 */
	String table();
}
