/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.exception;

import java.util.Arrays;

import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(CustomParameterized.class)
public abstract class BaseExceptionConversionTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {

	@Parameterized.Parameters(name = "{0}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList( new Object[][] {
				{ BootstrapMethod.JPA, ExceptionExpectations.jpa() },
				{ BootstrapMethod.NATIVE, ExceptionExpectations.nativePost52() }
		} );
	}

	protected final ExceptionExpectations exceptionExpectations;

	protected BaseExceptionConversionTest(BootstrapMethod bootstrapMethod,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod );
		this.exceptionExpectations = exceptionExpectations;
	}

}
