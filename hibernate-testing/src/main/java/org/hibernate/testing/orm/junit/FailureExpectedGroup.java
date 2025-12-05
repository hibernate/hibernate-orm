/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/// Grouping annotation for repeatable [@FailureExpected][FailureExpected] annotations.
///
/// @author Steve Ebersole
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)

@ExtendWith( FailureExpectedExtension.class )
public @interface FailureExpectedGroup {
	FailureExpected[] value();
}
