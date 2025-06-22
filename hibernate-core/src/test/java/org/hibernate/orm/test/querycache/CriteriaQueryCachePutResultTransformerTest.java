/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import org.hibernate.CacheMode;

/**
 * @author Gail Badner
 */
public class CriteriaQueryCachePutResultTransformerTest extends CriteriaQueryCacheIgnoreResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.PUT;
	}

	@Override
	protected boolean areDynamicNonLazyAssociationsChecked() {
		return false;
	}
}
