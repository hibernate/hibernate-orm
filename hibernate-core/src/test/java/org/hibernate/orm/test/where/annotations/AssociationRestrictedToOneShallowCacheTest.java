/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = {
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "true"),
		@Setting(name = "hibernate.cache.query_cache_layout", value = "SHALLOW")
})
class AssociationRestrictedToOneShallowCacheTest extends AssociationRestrictedToOneCacheTest {
}
