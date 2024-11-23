/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.querycache;

import org.hibernate.CacheMode;

/**
 * @author Gail Badner
 */
public class HqlQueryCachePutResultTransformerTest extends HqlQueryCacheIgnoreResultTransformerTest {
	@Override
	protected CacheMode getQueryCacheMode() {
		return CacheMode.PUT;
	}

	@Override
	protected boolean areDynamicNonLazyAssociationsChecked() {
		return false;
	}
}
