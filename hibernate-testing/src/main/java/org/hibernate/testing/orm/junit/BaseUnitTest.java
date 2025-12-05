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

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/// Applies standard set of JUnit Jupiter [extensions][org.junit.jupiter.api.extension.Extension]
/// useful for all testing.
///
/// @see FailureExpectedExtension
/// @see ExpectedExceptionExtension
/// @see DialectFilterExtension
///
/// @author Steve Ebersole
@Inherited
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
@TestInstance( TestInstance.Lifecycle.PER_CLASS )

@ExtendWith( FailureExpectedExtension.class )
@ExtendWith( ExpectedExceptionExtension.class )
@ExtendWith( DialectFilterExtension.class )
public @interface BaseUnitTest {

}
