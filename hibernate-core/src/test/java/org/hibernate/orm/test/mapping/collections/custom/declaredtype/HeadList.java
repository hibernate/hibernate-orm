/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import java.util.ArrayList;

/**
 * A custom collection class that implements a simple method just for illustration.
 * We extend a java.util.Collection class which is required for annotations-based entities, but not xml-based.
 *
 * @author David Weinberg
 */
public class HeadList<X> extends ArrayList<X> implements IHeadList<X> {

	@Override
	public X head() {
		return isEmpty() ? null : get( 0 );
	}
}
