/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.hamcrest;

import java.util.Collection;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * @author Steve Ebersole
 */
public class CollectionSizeMatcher extends BaseMatcher<Collection> {
	private final int size;

	public CollectionSizeMatcher(int size) {
		this.size = size;
	}

	@Override
	public boolean matches(Object item) {
		return ( (Collection) item ).size() == size;
	}

	@Override
	public void describeTo(Description description) {
		description.appendValue( size );
	}
}
