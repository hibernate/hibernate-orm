/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
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
import static org.hibernate.validator.constraints.CompositionType.OR;

/**
 * OR composition of {@code @Min(10)} and {@code @PositiveOrZero}.
 * <p>
 * Valid when value {@code >= 10} OR {@code >= 0}, i.e. effectively {@code >= 0}.
 * When combined with a direct {@code @Min} on the field, the OR result
 * should merge into the parent bounds rather than being applied separately.
 */
@ConstraintComposition(OR)
@Min(10)
@PositiveOrZero
@ReportAsSingleViolation
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {})
public @interface CustomMinOrPositiveOrZero {

	String message() default "must be >= 10 or >= 0";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
