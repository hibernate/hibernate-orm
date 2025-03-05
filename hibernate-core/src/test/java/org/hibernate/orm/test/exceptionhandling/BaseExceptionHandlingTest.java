/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import java.util.Arrays;
import java.util.Map;

import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(CustomParameterized.class)
public abstract class BaseExceptionHandlingTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {

	@Parameterized.Parameters(name = "Bootstrap={0}, ExceptionHandlingSetting={1}")
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

	@Override
	protected void configure(Map<String, Object> properties) {}
}
