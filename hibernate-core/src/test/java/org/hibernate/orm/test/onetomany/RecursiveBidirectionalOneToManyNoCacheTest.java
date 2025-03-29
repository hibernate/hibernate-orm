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
public class RecursiveBidirectionalOneToManyNoCacheTest extends AbstractRecursiveBidirectionalOneToManyTest {
	public String getCacheConcurrencyStrategy() {
			return null;
	}

	protected CacheMode getSessionCacheMode() {
			return CacheMode.IGNORE;
	}
}
