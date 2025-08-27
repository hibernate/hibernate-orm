/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import jakarta.persistence.DiscriminatorType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A simplified way to specify the type of the discriminator in an {@link Any}
 * mapping, using the JPA-defined {@link DiscriminatorType}. This annotation
 * must be used in combination with {@link jakarta.persistence.Column} to fully
 * describe the discriminator column for an {@code @Any} relationship.
 * <p>
 * {@code @AnyDiscriminator} is quite similar to
 * {@link jakarta.persistence.DiscriminatorColumn#discriminatorType()} in
 * single-table inheritance mappings, but it describes a discriminator held
 * along with the foreign key in the referring side of a discriminated
 * relationship.
 * <p>
 * This annotation may be used in conjunction with {@link JdbcType} or
 * {@link JdbcTypeCode} to more precisely specify the type mapping. On the
 * other hand, {@link JdbcType} or {@link JdbcTypeCode} may be used without
 * {@code @AnyDiscriminator}.
 *
 * @see Any
 * @see AnyDiscriminatorValue
 * @see AnyDiscriminatorImplicitValues
 *
 * @since 6.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention( RUNTIME )
public @interface AnyDiscriminator {
	/**
	 * The type of the discriminator, as a JPA {@link DiscriminatorType}.
	 * For more precise specification of the type, use {@link JdbcType}
	 * or {@link JdbcTypeCode}.
	 */
	DiscriminatorType value() default DiscriminatorType.STRING;
}
