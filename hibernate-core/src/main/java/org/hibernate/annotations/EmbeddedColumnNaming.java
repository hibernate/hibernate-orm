/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Allows specifying a pattern to be applied to the naming of columns for
 * a particular {@linkplain jakarta.persistence.Embedded embedded mapping}.
 * For example, given a typical embeddable named {@code Address} and
 * {@code @EmbeddedColumnNaming("home_%s)}, we will get columns named
 * {@code home_street}, {@code home_city}, etc.
 * <p/>
 * Explicit {@linkplain jakarta.persistence.Column @Column(name)} mappings are incorporated
 * into the result.  When embeddables are nested, the affect will be cumulative.  Given the following model:
 *
 * <pre>
 * &#64;Entity
 * class Person {
 *     ...
 *     &#64;Embedded
 *     &#64;EmbeddedColumnNaming("home_%s")
 *     Address homeAddress;
 *     &#64;Embedded
 *     &#64;EmbeddedColumnNaming("work_%s")
 *     Address workAddress;
 * }
 *
 * &#64;Embeddable
 * class Address {
 *     &#64;Column(name="line1")
 *     String street;
 *     ...
 *     &#64;Embedded
 *     &#64;EmbeddedColumnNaming("zip_%s")
 *     ZipPlus4 zip;
 * }
 *
 * &#64;Embeddable
 * class ZipPlus4 {
 *    &#64;Column(name="zip_code")
 *    String zipCode;
 *    &#64;Column(name="plus_code")
 *     String plusCode;
 * }
 * </pre>
 * Will result in the following columns:<ol>
 *     <li>{@code home_line1}</li>
 *     <li>{@code home_zip_zip_code}</li>
 *     <li>{@code home_zip_plus_code}</li>
 *     <li>{@code work_line1}</li>
 *     <li>{@code work_zip_zip_code}</li>
 *     <li>{@code work_zip_plus_code}</li>
 * </ol>
 *
 * @since 7.0
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Incubating
public @interface EmbeddedColumnNaming {
	/**
	 * The naming pattern.  It is expected to contain a single pattern marker ({@code %})
	 * into which the "raw" column name will be injected.
	 * <p/>
	 * The {@code value} may be omitted which will indicate to use the pattern
	 * {@code "{ATTRIBUTE_NAME}_%s"} where {@code {ATTRIBUTE_NAME}} is the name of the attribute
	 * where the annotation is placed - e.g. {@code @Embedded @EmbeddedColumnNaming Address homeAddress}
	 * would create columns {@code homeAddress_street}, {@code homeAddress_city}, etc.
	 */
	String value() default "";
}
