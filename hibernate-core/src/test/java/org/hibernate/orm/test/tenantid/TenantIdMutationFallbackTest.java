package org.hibernate.orm.test.tenantid;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;

/**
 * Exercise the separate update/insert fallback as well as native MERGE.
 */
@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = { TenantIdMutationTest.Item.class, TenantIdMutationTest.PlainItem.class, TenantIdMutationTest.Owner.class, TenantIdMutationTest.PartitionedItem.class, TenantIdMutationTest.SoftItem.class })
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = DIALECT, value = "org.hibernate.orm.test.tenantid.TenantIdMutationFallbackTest$FallbackDialect"),
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdMutationFallbackTest extends TenantIdMutationTest {
	public static class FallbackDialect extends H2Dialect {
		@Override
		public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
			return new OptionalTableUpdateOperation( request.update().getMutationTarget(), request.update() );
		}
	}
}
