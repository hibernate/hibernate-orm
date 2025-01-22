/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.usertype.UserType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies a custom {@link UserType} for the annotated attribute mapping.
 * This annotation may be applied:
 * <ul>
 * <li>directly to a property or field of an entity to specify the custom
 *     type of the property or field,
 * <li>indirectly, as a meta-annotation of an annotation type that is then
 *     applied to various properties and fields, or
 * <li>by default, via a {@linkplain TypeRegistration registration}.
 * </ul>
 * <p>
 * For example, as an alternative to:
 * <pre>
 * &#64;Type(MonetaryAmountUserType.class)
 * BigDecimal amount;
 * </pre>
 * <p>
 * we may define a custom annotation type:
 * <pre>
 * &#64;Retention(RUNTIME)
 * &#64;Target({METHOD,FIELD})
 * &#64;Type(MonetaryAmountUserType.class)
 * public @interface MonetaryAmount {}
 * </pre>
 * <p>
 * and then write:
 * <pre>
 * &#64;MonetaryAmount
 * BigDecimal amount;
 * </pre>
 * <p>
 * which is much cleaner.
 * <p>
 * An implementation of {@link UserType} applied via a custom annotation
 * may declare a constructor which accepts the annotation instance,
 * allowing the annotation to be used to configure the type.
 * <pre>
 * &#64;Retention(RUNTIME)
 * &#64;Target({METHOD,FIELD})
 * &#64;Type(MonetaryAmountUserType.class)
 * public @interface MonetaryAmount {
 *     public Currency currency();
 * }
 * </pre>
 * <pre>
 * public class MonetaryAmountUserType implements UserType&lt;Amount&gt; {
 *     private final Currency currency;
 *     public MonetaryAmountUserType(MonetaryAmount annotation) {
 *         currency = annotation.currency();
 *     }
 *     ...
 * }
 * </pre>
 * <p>
 * The use of a {@code UserType} is usually mutually exclusive with the
 * compositional approach of {@link JavaType} and {@link JdbcType}.
 *
 * @see UserType
 * @see TypeRegistration
 * @see CompositeType
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface Type {
	/**
	 * The class which implements {@link UserType}.
	 */
	Class<? extends UserType<?>> value();

	/**
	 * Parameters to be injected into the custom type after it is
	 * instantiated. The {@link UserType} implementation must implement
	 * {@link org.hibernate.usertype.ParameterizedType} to receive the
	 * parameters.
	 */
	Parameter[] parameters() default {};
}
