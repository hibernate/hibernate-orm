/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.binder.internal.AttributeAccessorBinder;
import org.hibernate.property.access.spi.PropertyAccessStrategy;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies an attribute {@linkplain PropertyAccessStrategy access strategy} to use.
 * <p>
 * Can be specified at either:<ul>
 *     <li>
 *         <strong>TYPE</strong> level, which will act as the default accessor strategy for
 *         all attributes on the class which do not explicitly name an accessor strategy
 *     </li>
 *     <li>
 *         <strong>METHOD/FIELD</strong> level, which will be in effect for just that attribute.
 *     </li>
 * </ul>
 * <p>
 * Should only be used to name custom {@link PropertyAccessStrategy}.  For
 * {@code property/field} access, the JPA {@link jakarta.persistence.Access} annotation should be preferred
 * using the appropriate {@link jakarta.persistence.AccessType}.  However, if this annotation is used with
 * either {@code value="property"} or {@code value="field"}, it will act just as the corresponding usage
 * of {@link jakarta.persistence.Access}.
 *
 * @see PropertyAccessStrategy
 * @see org.hibernate.property.access.spi.PropertyAccessStrategyResolver
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@AttributeBinderType(binder = AttributeAccessorBinder.class)
public @interface AttributeAccessor {
	/**
	 * Names the {@link PropertyAccessStrategy} strategy.
	 *
	 * @deprecated use {@link #strategy()}
	 */
	@Deprecated(since = "6.0", forRemoval = true)
	String value() default "";
	/**
	 * A class implementing {@link PropertyAccessStrategy}.
	 */
	Class<? extends PropertyAccessStrategy> strategy() default PropertyAccessStrategy.class;
}
