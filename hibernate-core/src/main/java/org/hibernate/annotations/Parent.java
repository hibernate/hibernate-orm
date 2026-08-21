/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that the annotated field or property of an embeddable
 * class is a reference to the owning entity.
 * <pre>
 * &#64;Entity
 * class Course {
 *     &#64;Id @GeneratedValue id;
 *     &#64;Embedded CourseCode code;
 *     ...
 * }
 *
 * &#64;Embeddable
 * class CourseCode {
 *     &#64;Parent Course course;
 *     String prefix;
 *     String numericSuffix;
 * }
 * </pre>
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Parent {
}
