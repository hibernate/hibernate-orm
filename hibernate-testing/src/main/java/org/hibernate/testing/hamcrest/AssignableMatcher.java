/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.hamcrest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * @author Steve Ebersole
 */
public class AssignableMatcher extends BaseMatcher<Class> {
	public static AssignableMatcher assignableTo(Class expected) {
		return new AssignableMatcher( expected );
	}

	private final Class<?> expected;

	public AssignableMatcher(Class<?> expected) {
		this.expected = expected;
	}

	@Override
	public boolean matches(Object item) {
		return expected.isAssignableFrom( (Class) item );
	}

	@Override
	public void describeTo(Description description) {
		description.appendText( "assignable to " ).appendText( expected.getName() );
	}
}
