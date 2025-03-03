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
public class CollectionEmptinessMatcher extends BaseMatcher<Collection> {
	private final boolean emptyIsMatch;

	public CollectionEmptinessMatcher(boolean emptyIsMatch) {
		this.emptyIsMatch = emptyIsMatch;
	}

	@Override
	public boolean matches(Object item) {
		if ( emptyIsMatch ) {
			return ( (Collection) item ).isEmpty();
		}
		else {
			return ! ( (Collection) item ).isEmpty();
		}
	}

	@Override
	public void describeTo(Description description) {
		if ( emptyIsMatch ) {
			description.appendText( "<is empty>" );
		}
		else {
			description.appendText( "<is not empty>" );
		}
	}
}
