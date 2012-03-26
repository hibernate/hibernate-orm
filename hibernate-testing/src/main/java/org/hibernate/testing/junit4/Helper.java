/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing.junit4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import org.hibernate.testing.FailureExpected;

/**
 * Centralized utility functionality
 *
 * @author Steve Ebersole
 */
public class Helper {
	public static final String VALIDATE_FAILURE_EXPECTED = "hibernate.test.validatefailureexpected";


	/**
	 * Standard string content checking.
	 *
	 * @param string The string to check
	 * @return Are its content empty or the reference null?
	 */
	public static boolean isNotEmpty(String string) {
		return string != null && string.length() > 0;
	}

	/**
	 * Extract a nice test name representation for display
	 *
	 * @param frameworkMethod The test method.
	 * @return The display representation
	 */
	public static String extractTestName(FrameworkMethod frameworkMethod) {
		return frameworkMethod.getMethod().getDeclaringClass().getName() + '#' + frameworkMethod.getName();
	}

	/**
	 * Extract a nice method name representation for display
	 *
	 * @param method The method.
	 * @return The display representation
	 */
	public static String extractMethodName(Method method) {
		return method.getDeclaringClass().getName() + "#" + method.getName();
	}

	public static <T extends Annotation> T locateAnnotation(
			Class<T> annotationClass,
			FrameworkMethod frameworkMethod,
			TestClass testClass) {
		T annotation = frameworkMethod.getAnnotation( annotationClass );
		if ( annotation == null ) {
			annotation = testClass.getJavaClass().getAnnotation( annotationClass );
		}
		return annotation;
	}

	public static String extractMessage(FailureExpected failureExpected) {
		StringBuilder buffer = new StringBuilder();
		buffer.append( '(' ).append( failureExpected.jiraKey() ).append( ')' );
		if ( isNotEmpty( failureExpected.message() ) ) {
			buffer.append( " : " ).append( failureExpected.message() );
		}
		return buffer.toString();
	}

	public static String extractIgnoreMessage(FailureExpected failureExpected, FrameworkMethod frameworkMethod) {
		return new StringBuilder( "Ignoring test [" )
				.append( Helper.extractTestName( frameworkMethod ) )
				.append( "] due to @FailureExpected - " )
				.append( extractMessage( failureExpected ) )
				.toString();
	}

}
