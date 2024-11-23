/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;

import org.hibernate.CacheMode;

/**
 * @author Burkhard Graves
 * @author Gail Badner
 */
public class RecursiveVersionedBidirectionalOneToManyNoCacheTest extends AbstractVersionedRecursiveBidirectionalOneToManyTest {
	public String getCacheConcurrencyStrategy() {
			return null;
	}

	protected CacheMode getSessionCacheMode() {
			return CacheMode.IGNORE;
	}
}
