/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.Incubating;
import org.junit.jupiter.api.extension.AfterClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedMethods;
import static org.junit.platform.commons.support.HierarchyTraversalMode.BOTTOM_UP;

/**
 * JUnit extension to provide hooks for methods annotated with {@link BeforeClassTemplate}
 * and {@link AfterClassTemplate} to be called before and after each class template invocation
 * <p>
 * Provides native support for methods with zero parameters or a single parameter typed either
 * as {@link EntityManagerFactoryScope} or {@link SessionFactoryScope}, for easy integration
 * with the {@link EntityManagerFactoryExtension entity manager} and {@link SessionFactoryExtension
 * session factory} extensions.
 */
@Incubating
public class ClassTemplateInvocationListenersExtension
		implements BeforeClassTemplateInvocationCallback, AfterClassTemplateInvocationCallback {
	@Override
	public void beforeClassTemplateInvocation(ExtensionContext context) throws Exception {
		final var testClass = context.getRequiredTestClass();
		final var annotatedMethods = findAnnotatedMethods( testClass, BeforeClassTemplate.class, BOTTOM_UP );
		for ( final var method : annotatedMethods ) {
			context.getExecutableInvoker().invoke( method, context.getRequiredTestInstance() );
		}
	}

	@Override
	public void afterClassTemplateInvocation(ExtensionContext context) throws Exception {
		final var testClass = context.getRequiredTestClass();
		final var annotatedMethods = findAnnotatedMethods( testClass, AfterClassTemplate.class, BOTTOM_UP );
		for ( final var method : annotatedMethods ) {
			context.getExecutableInvoker().invoke( method, context.getRequiredTestInstance() );
		}
	}
}
