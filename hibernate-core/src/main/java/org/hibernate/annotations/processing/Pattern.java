/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.processing;

import jakarta.persistence.criteria.Expression;
import org.hibernate.Incubating;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Indicates that a parameter of type {@link String} of a
 * {@linkplain Find finder method} is a pattern involving
 * wildcard characters {@code _} and {@code %}.
 * <p>
 * For example:
 * <pre>
 * &#064;Find
 * List&lt;Book&gt; getBooksWithTitle(@Pattern String title);
 * </pre>
 * <p>
 * A parameter annotated {@code @Pattern} results in a
 * {@link jakarta.persistence.criteria.CriteriaBuilder#like(Expression, String) like}
 * condition in the generated code.
 *
 * @see Find
 *
 * @since 6.5
 * @author Gavin King
 */
@Target(PARAMETER)
@Retention(CLASS)
@Incubating
public @interface Pattern {
}
