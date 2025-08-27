/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Registers an {@link jakarta.persistence.AttributeConverter}.  The main
 * purpose is to be able to control {@link jakarta.persistence.Converter#autoApply()}
 * external to the actual converter class, which might be useful for third-parties
 * converters.
 *
 * @author Steve Ebersole
 */
@Target( {TYPE, ANNOTATION_TYPE, PACKAGE} )
@Retention( RUNTIME )
@Repeatable( ConverterRegistrations.class )
public @interface ConverterRegistration {
	/**
	 * The converter class to register
	 */
	Class<? extends AttributeConverter<?,?>> converter();

	/**
	 * The domain type to which this converter should be applied.  This allows
	 * refining the domain type associated with the converter e.g. to apply to
	 * a subtype.
	 * <p>
	 * With {@link #autoApply()} set to true, this will effectively override converters
	 * defined with {@link Converter#autoApply()} set to {@code false} and auto-apply them.
	 * <p>
	 * With {@link #autoApply()} set to false, this will effectively override converters
	 * defined with {@link Converter#autoApply()} set to {@code true} and disable auto-apply
	 * for them.
	 */
	Class<?> domainType() default void.class;

	/**
	 * Should the registered converter be auto applied for
	 * converting values of its reported domain type?
	 * <p>
	 * Defaults to true as that is the more common use case for this annotation.
	 */
	boolean autoApply() default true;
}
