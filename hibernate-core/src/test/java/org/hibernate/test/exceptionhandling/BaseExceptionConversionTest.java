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
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.CustomParameterized;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(CustomParameterized.class)
public abstract class BaseExceptionConversionTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {
	// FIXME use the actual setting
	private static final String EXCEPTION_CONVERSION_PROPERTY =
			AvailableSettings.NATIVE_EXCEPTION_HANDLING_51_COMPLIANCE;

	public enum ExceptionConversionSetting {
		DEFAULT,
		JPA,
		NATIVE_PRE_52,
		NATIVE_POST_52
	}

	@Parameterized.Parameters(name = "Bootstrap={0}, ConversionSetting={1}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList( new Object[][] {
				{ BootstrapMethod.JPA, ExceptionConversionSetting.DEFAULT, ExceptionExpectations.jpa() },
				{ BootstrapMethod.JPA, ExceptionConversionSetting.JPA, ExceptionExpectations.jpa() },
				{ BootstrapMethod.JPA, ExceptionConversionSetting.NATIVE_PRE_52, ExceptionExpectations.jpa() },
				{ BootstrapMethod.JPA, ExceptionConversionSetting.NATIVE_POST_52, ExceptionExpectations.jpa() },
				{ BootstrapMethod.NATIVE, ExceptionConversionSetting.DEFAULT, ExceptionExpectations.nativePost52() },
				{ BootstrapMethod.NATIVE, ExceptionConversionSetting.NATIVE_PRE_52, ExceptionExpectations.nativePre52() },
				{ BootstrapMethod.NATIVE, ExceptionConversionSetting.NATIVE_POST_52, ExceptionExpectations.nativePost52() }
		} );
	}

	private final ExceptionConversionSetting exceptionConversionSetting;

	protected final ExceptionExpectations exceptionExpectations;

	protected BaseExceptionConversionTest(BootstrapMethod bootstrapMethod,
			ExceptionConversionSetting exceptionConversionSetting,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod );
		this.exceptionConversionSetting = exceptionConversionSetting;
		this.exceptionExpectations = exceptionExpectations;
	}

	@Override
	protected void configure(Map<Object, Object> properties) {
		switch ( exceptionConversionSetting ) {
			case DEFAULT:
				// Keep the default
				break;
			case JPA:
				properties.put( EXCEPTION_CONVERSION_PROPERTY, "false" );
				break;
			case NATIVE_PRE_52:
				properties.put( EXCEPTION_CONVERSION_PROPERTY, "true" );
				break;
			case NATIVE_POST_52:
				properties.put( EXCEPTION_CONVERSION_PROPERTY, "false" );
				break;
		}
	}
}
