/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.ConstraintComposition;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hibernate.validator.constraints.CompositionType.AND;

/**
 * AND composition of {@code @Min(5)} and {@code @Positive}.
 * <p>
 * Since both are lower bounds and the composition is AND,
 * only the most restrictive ({@code >= 5}) should be applied.
 */
@ConstraintComposition(AND)
@Min(5)
@Positive
@ReportAsSingleViolation
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {})
public @interface CustomMinAndPositive {

	String message() default "must be >= 5 and positive";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
