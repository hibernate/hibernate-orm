/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.junit4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.testing.FailureExpected;
import org.junit.Ignore;
import org.junit.runners.model.FrameworkMethod;

/**
 * Defines an extension to the standard JUnit {@link FrameworkMethod} information about a test method.
 *
 * @author Steve Ebersole
 */
public class ExtendedFrameworkMethod extends FrameworkMethod {
	private final FrameworkMethod delegatee;
	private final Ignore virtualIgnore;
	private final FailureExpected failureExpectedAnnotation;

	public ExtendedFrameworkMethod(FrameworkMethod delegatee, Ignore virtualIgnore, FailureExpected failureExpectedAnnotation) {
		super( delegatee.getMethod() );
		this.delegatee = delegatee;
		this.virtualIgnore = virtualIgnore;
		this.failureExpectedAnnotation = failureExpectedAnnotation;
	}

	public FailureExpected getFailureExpectedAnnotation() {
		return failureExpectedAnnotation;
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
	@SuppressWarnings( {"EqualsWhichDoesntCheckParameterClass"})
	public boolean equals(Object obj) {
		return delegatee.equals( obj );
	}

	@Override
	public int hashCode() {
		return delegatee.hashCode();
	}

	@Override
	public Annotation[] getAnnotations() {
		return delegatee.getAnnotations();
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
		if ( Ignore.class.equals( annotationType ) && virtualIgnore != null ) {
			return (T) virtualIgnore;
		}
		return delegatee.getAnnotation( annotationType );
	}
}
