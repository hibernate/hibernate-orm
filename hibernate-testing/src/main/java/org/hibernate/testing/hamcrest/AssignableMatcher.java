/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.hamcrest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * @author Steve Ebersole
 */
public class AssignableMatcher extends BaseMatcher<Class<?>> {
	public static final AssignableMatcher assignableTo(Class<?> expected) {
		return new AssignableMatcher( expected );
	}

	private final Class<?> expected;

	public AssignableMatcher(Class<?> expected) {
		this.expected = expected;
	}

	@Override
	public boolean matches(Object item) {
		return expected.isAssignableFrom( item.getClass() );
	}

	@Override
	public void describeTo(Description description) {
		description.appendText( "assignable to " ).appendText( expected.getName() );
	}
}
