/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import org.jboss.logging.Logger;

/**
 * TestExecutionExceptionHandler used in conjunction with {@link ExpectedException}
 * to support annotating tests with a specific exception that indicates a
 * success (we are expecting that exception in that tested condition).
 *
 * @see ExpectedException
 *
 * @author Steve Ebersole
 */
public class ExpectedExceptionExtension implements TestExecutionExceptionHandler {
	private static final Logger log = Logger.getLogger( ExpectedExceptionExtension.class );

	@Override
	public void handleTestExecutionException(
			ExtensionContext context,
			Throwable throwable) throws Throwable {
		final ExpectedException annotation = context.getRequiredTestMethod().getAnnotation( ExpectedException.class );
		if ( annotation != null ) {
			if ( annotation.value().isInstance( throwable ) ) {
				log.debugf(
						"Test [%s] threw exception [%s] which matched @ExpectedException : swallowing exception",
						context.getDisplayName(),
						throwable
				);
				return;
			}
		}

		throw throwable;
	}
}
