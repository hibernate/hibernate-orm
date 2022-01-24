/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Simplified form for describing the discriminator value mapping as a discrete
 * set.  Follows the pattern of JPA's {@link DiscriminatorColumn#discriminatorType()}.
 * <p/>
 * Can be used in conjunction with {@link JdbcType} or {@link JdbcTypeCode} to
 * further describe the underlying mapping.  {@link JdbcType} or {@link JdbcTypeCode}
 * can also be used without AnyDiscriminator.
 *
 * @see Any
 *
 * @since 6.0
 */
@java.lang.annotation.Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention( RUNTIME )
public @interface AnyDiscriminator {
	/**
	 * The simplified discriminator value mapping
	 */
	DiscriminatorType value() default DiscriminatorType.STRING;
}
