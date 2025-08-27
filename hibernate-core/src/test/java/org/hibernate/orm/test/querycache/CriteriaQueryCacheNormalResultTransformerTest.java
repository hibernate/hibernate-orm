/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import org.hibernate.CacheMode;

/**
 * @author Gail Badner
 */
public class CriteriaQueryCacheNormalResultTransformerTest extends CriteriaQueryCachePutResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.NORMAL;
	}
}
