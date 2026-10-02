package org.hibernate.orm.test.tenantid;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

@ServiceRegistry(settings = {
		@Setting(name = "hibernate.flush.queue.type", value = "legacy"),
		@Setting(name = "hibernate.tenant_identifier_resolver", value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = "hibernate.multi_tenant.rls_enabled", value = "false"),
		@Setting(name = "hibernate.cache.use_second_level_cache", value = "true"),
		@Setting(name = "hibernate.cache.use_query_cache", value = "true"),
		@Setting(name = "hibernate.cache.region.factory_class", value = "org.hibernate.testing.cache.CachingRegionFactory")
})
class RootTenantCacheLegacyTest extends RootTenantCacheTest {
}
