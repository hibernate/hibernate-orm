/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
