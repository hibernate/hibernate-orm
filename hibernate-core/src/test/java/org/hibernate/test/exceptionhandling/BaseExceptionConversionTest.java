/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.exceptionhandling;

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
