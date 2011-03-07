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

import org.hibernate.testing.FailureExpected;
import org.junit.runners.model.FrameworkMethod;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Defines an extension to the standard JUnit {@link FrameworkMethod} information about a test method.
 *
 * @author Steve Ebersole
 */
public class ExtendedFrameworkMethod extends FrameworkMethod {
	private static final Object[] NO_ARGS = new Object[0];

	private final FrameworkMethod delegatee;
    private final FailureExpected failureExpectedAnnotation;
	private final TestClassCallbackMetadata callbackMetadata;
	private final CustomRunner unitRunner;

	public ExtendedFrameworkMethod(
			FrameworkMethod delegatee,
			FailureExpected failureExpectedAnnotation,
			TestClassCallbackMetadata callbackMetadata,
			CustomRunner unitRunner) {
		super( delegatee.getMethod() );
		this.delegatee = delegatee;
		this.failureExpectedAnnotation = failureExpectedAnnotation;
		this.callbackMetadata = callbackMetadata;
		this.unitRunner = unitRunner;
	}

	public CustomRunner getUnitRunner() {
		return unitRunner;
	}

    public boolean isMarkedAsFailureExpected() {
        return failureExpectedAnnotation != null;
    }

    public FailureExpected getFailureExpectedAnnotation() {
        return failureExpectedAnnotation;
    }

    public TestClassCallbackMetadata getCallbackMetadata() {
        return callbackMetadata;
    }

    @Override
	public Method getMethod() {
		return delegatee.getMethod();
	}

	@Override
	public Object invokeExplosively(Object target, Object... params) throws Throwable {
		return delegatee.invokeExplosively( target, params );
	}

	@Override
	public String getName() {
		return delegatee.getName();
	}

	@Override
	public void validatePublicVoidNoArg(boolean isStatic, List<Throwable> errors) {
		delegatee.validatePublicVoidNoArg( isStatic, errors );
	}

	@Override
	public void validatePublicVoid(boolean isStatic, List<Throwable> errors) {
		delegatee.validatePublicVoid( isStatic, errors );
	}

	@Override
	public boolean isShadowedBy(FrameworkMethod other) {
		return delegatee.isShadowedBy( other );
	}

	@Override
	public boolean equals(Object obj) {
		return delegatee.equals( obj );
	}

	@Override
	public int hashCode() {
		return delegatee.hashCode();
	}

	@Override
	public boolean producesType(Class<?> type) {
		return delegatee.producesType( type );
	}

	@Override
	public Annotation[] getAnnotations() {
		return delegatee.getAnnotations();
	}

	@Override
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		return delegatee.getAnnotation( annotationType );
	}
}
