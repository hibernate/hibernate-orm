/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.Incubating;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Explicitly selects a standard optimizer to use with identifier generation.
 * <p>
 * <pre>
 * &#64;Id
 * &#64;GeneratedValue(strategy = GenerationType.SEQUENCE)
 * &#64;SequenceGenerator(sequenceName = "optimized_sequence", allocationSize = 20)
 * &#64;Optimizer(StandardOptimizerDescriptor.POOLED_LO)
 * </pre>
 * <p>
 * Intended for use with {@link jakarta.persistence.GeneratedValue}, usually
 * in combination with {@link jakarta.persistence.SequenceGenerator} or
 * {@link jakarta.persistence.TableGenerator}.
 *
 * @author Gavin King
 *
 * @since 7.4
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
@Incubating
public @interface Optimizer {
	StandardOptimizerDescriptor value();
}
