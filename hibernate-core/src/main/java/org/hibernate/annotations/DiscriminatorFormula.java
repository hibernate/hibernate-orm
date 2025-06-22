/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import jakarta.persistence.DiscriminatorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static jakarta.persistence.DiscriminatorType.STRING;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies an expression written in native SQL as the discriminator for an
 * entity inheritance hierarchy. Must be used to annotate the root entity of
 * the hierarchy.
 * <p>
 * Used in place of the JPA {@link jakarta.persistence.DiscriminatorColumn}.
 * <p>
 * For example, we might declare a supertype as follows:
 * <pre>
 * &#64;Entity
 * &#64;DiscriminatorFormula(discriminatorType = INTEGER,
 * 		value = "case when value1 is not null then 1 when value2 is not null then 2 end")
 * public abstract class AbstractChild {
 *     &#64;Id
 *     &#64;GeneratedValue
 *     Integer id;
 *     ...
 * }
 * </pre>
 * <p>
 * and then each concrete subclass must specify a matching discriminator value:
 * <pre>
 * &#64;Entity
 * &#64;DiscriminatorValue("1")
 * public class ConcreteChild1 extends AbstractChild {
 *     &#64;Basic(optional = false)
 *     &#64;Column(name = "VALUE1")
 *     String value;
 *     ...
 * }
 * </pre>
 * <pre>
 * &#64;Entity
 * &#64;DiscriminatorValue("2")
 * public class ConcreteChild2 extends AbstractChild {
 *     &#64;Basic(optional = false)
 *     &#64;Column(name = "VALUE2")
 *     String value;
 *     ...
 * }
 * </pre>
 *
 * @see Formula
 * @see DialectOverride.DiscriminatorFormula
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface DiscriminatorFormula {
	/**
	 * The formula string.
	 */
	String value();

	/**
	 * The type of value returned by the formula.
	 * <p>
	 * This is required, unless the {@linkplain #value()
	 * expression} is of type {@code varchar} or similar.
	 */
	DiscriminatorType discriminatorType() default STRING;
}
