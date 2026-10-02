package org.hibernate.orm.test.tenantid;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;

@DomainModel(annotatedClasses = { TenantIdTemporalMutationTest.Item.class, TenantIdTemporalMutationTest.PlainItem.class,
		TenantIdTemporalMutationTest.CompositeItem.class })
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"),
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false"),
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "SINGLE_TABLE")
})
class TenantIdTemporalMutationLegacyTest extends TenantIdTemporalMutationTest {
}
