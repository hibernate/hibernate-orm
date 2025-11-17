/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to identify a method as a callback to be executed whenever a {@link FailureExpected} is handled.
 *
 * @author Steve Ebersole
 * @deprecated No replacement with JUnit Jupiter at the moment.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Deprecated(forRemoval = true)
public @interface OnExpectedFailure {
}
