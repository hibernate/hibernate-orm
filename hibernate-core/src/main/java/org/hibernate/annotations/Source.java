/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.generator.internal.SourceGeneration;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates the source of timestamps for an entity
 * {@linkplain jakarta.persistence.Version version property} of
 * type {@link java.sql.Timestamp}:
 * <ul>
 * <li>{@link SourceType#VM} indicates that the virtual machine
 *     {@linkplain java.time.Clock#instant() current instance}
 *     is used, and
 * <li>{@link SourceType#DB} indicates that the database
 *     {@code current_timestamp} function should be used.
 * </ul>
 * <p>
 * For example, the following timestamp is generated by the
 * database:
 * <pre>
 * &#64;Version &#64;Source(DB)
 * private LocalDateTime version;
 * </pre>
 * <p>
 * This annotation is always used in conjunction with the JPA
 * {@link jakarta.persistence.Version @Version} annotation.
 *
 * @author Hardy Ferentschik
 *
 * @see jakarta.persistence.Version
 *
 * @deprecated use {@link CurrentTimestamp} instead
 */
@Deprecated(since = "6.2", forRemoval = true)
@Target({ METHOD, FIELD })
@Retention(RUNTIME)
@ValueGenerationType(generatedBy = SourceGeneration.class)
public @interface Source {
	/**
	 * The source of timestamps. By default, the {@linkplain
	 * SourceType#VM virtual machine} is the source.
	 */
	SourceType value() default SourceType.VM;
}
