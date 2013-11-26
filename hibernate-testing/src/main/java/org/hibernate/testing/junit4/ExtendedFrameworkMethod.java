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
