/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.onetomany;

import org.hibernate.CacheMode;

/**
 * @author Burkhard Graves
 * @author Gail Badner
 */
public class RecursiveVersionedBidirectionalOneToManyCacheTest extends AbstractVersionedRecursiveBidirectionalOneToManyTest {
	@Override
	protected CacheMode getSessionCacheMode() {
			return CacheMode.NORMAL;
	}
}
