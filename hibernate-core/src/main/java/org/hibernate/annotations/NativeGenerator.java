/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import org.hibernate.dialect.Dialect;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Generator that picks a strategy based on the {@linkplain Dialect#getNativeValueGenerationStrategy() dialect}.
 *
 * @since 7.0
 *
 * @author Steve Ebersole
 */
@Target({METHOD, FIELD, TYPE, PACKAGE})
@Retention(RUNTIME)
@IdGeneratorType(org.hibernate.id.NativeGenerator.class)
public @interface NativeGenerator {
	SequenceGenerator sequenceForm() default @SequenceGenerator();
	TableGenerator tableForm() default @TableGenerator();
}
