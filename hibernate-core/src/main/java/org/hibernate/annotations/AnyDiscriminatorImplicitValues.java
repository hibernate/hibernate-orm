/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.Incubating;
import org.hibernate.metamodel.internal.FullNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.internal.ShortNameImplicitDiscriminatorStrategy;
import org.hibernate.metamodel.spi.ImplicitDiscriminatorStrategy;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines how to handle {@linkplain AnyDiscriminator discriminator} values which are not explicitly
 * mapped with {@linkplain AnyDiscriminatorValue}.
 *
 * @author Steve Ebersole
 * @since 7.0
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE})
@Retention( RUNTIME )
@Incubating
public @interface AnyDiscriminatorImplicitValues {
	enum Strategy {
		/**
		 * Use the {@link ImplicitDiscriminatorStrategy} implementation specified by {@link #implementation()}
		 */
		CUSTOM,
		/**
		 * Use the entity's short-name.
		 *
		 * @see ShortNameImplicitDiscriminatorStrategy
		 */
		SHORT_NAME,
		/**
		 * Use the entity's full-name.
		 *
		 * @see FullNameImplicitDiscriminatorStrategy
		 */
		FULL_NAME
	}

	/**
	 * The type of strategy to use.  This is {@link Strategy#CUSTOM} by default and
	 * the class named by {@link #implementation()} is used.
	 */
	Strategy value() default Strategy.CUSTOM;

	/**
	 * Specific strategy implementation to use, when combined with {@code value=CUSTOM}
	 */
	Class<? extends ImplicitDiscriminatorStrategy> implementation() default ImplicitDiscriminatorStrategy.class;
}
