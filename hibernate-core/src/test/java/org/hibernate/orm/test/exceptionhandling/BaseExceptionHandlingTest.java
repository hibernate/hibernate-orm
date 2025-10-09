/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

@ParameterizedClass
@MethodSource("parameters")
public abstract class BaseExceptionHandlingTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {

	public static Iterable<Object[]> parameters() {
		return Arrays.asList( new Object[][] {
				{ BootstrapMethod.JPA, ExceptionExpectations.jpa() },
				{ BootstrapMethod.NATIVE, ExceptionExpectations.nativePost52() },
		} );
	}

	protected final ExceptionExpectations exceptionExpectations;

	protected BaseExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod );
		this.exceptionExpectations = exceptionExpectations;
	}
}
