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
public abstract class BaseExceptionHandlingTest extends BaseJpaOrNativeBootstrapFunctionalTestCase {

	private static final String EXCEPTION_HANDLING_PROPERTY =
			AvailableSettings.NATIVE_EXCEPTION_HANDLING_51_COMPLIANCE;

	public enum ExceptionHandlingSetting {
		DEFAULT,
		TRUE,
		FALSE
	}

	@Parameterized.Parameters(name = "Bootstrap={0}, ExceptionHandlingSetting={1}")
	public static Iterable<Object[]> parameters() {
		return Arrays.asList( new Object[][] {
				{ BootstrapMethod.JPA, ExceptionHandlingSetting.DEFAULT, ExceptionExpectations.jpa() },
				{ BootstrapMethod.JPA, ExceptionHandlingSetting.TRUE, ExceptionExpectations.jpa() },
				{ BootstrapMethod.JPA, ExceptionHandlingSetting.FALSE, ExceptionExpectations.jpa() },
				{ BootstrapMethod.NATIVE, ExceptionHandlingSetting.DEFAULT, ExceptionExpectations.nativePost52() },
				{ BootstrapMethod.NATIVE, ExceptionHandlingSetting.TRUE, ExceptionExpectations.nativePre52() },
				{ BootstrapMethod.NATIVE, ExceptionHandlingSetting.FALSE, ExceptionExpectations.nativePost52() }
		} );
	}

	private final ExceptionHandlingSetting exceptionHandlingSetting;

	protected final ExceptionExpectations exceptionExpectations;

	protected BaseExceptionHandlingTest(
			BootstrapMethod bootstrapMethod,
			ExceptionHandlingSetting exceptionHandlingSetting,
			ExceptionExpectations exceptionExpectations) {
		super( bootstrapMethod );
		this.exceptionHandlingSetting = exceptionHandlingSetting;
		this.exceptionExpectations = exceptionExpectations;
	}

	@Override
	protected void configure(Map<Object, Object> properties) {
		switch ( exceptionHandlingSetting ) {
			case DEFAULT:
				// Keep the default
				break;
			case TRUE:
				properties.put( EXCEPTION_HANDLING_PROPERTY, "true" );
				break;
			case FALSE:
				properties.put( EXCEPTION_HANDLING_PROPERTY, "false" );
				break;
		}
	}
}