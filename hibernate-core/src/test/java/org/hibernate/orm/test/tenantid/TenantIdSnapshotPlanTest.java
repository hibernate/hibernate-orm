package org.hibernate.orm.test.tenantid;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TenantId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.hibernate.orm.test.tenantid.TenantIdMutationMappingTest.inTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = TenantIdSnapshotPlanTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = DIALECT, value = "org.hibernate.orm.test.tenantid.TenantIdSnapshotPlanTest$CountingDialect"),
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER, value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdSnapshotPlanTest {
	@Test
	void snapshotsTranslateOncePerFilterShape(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Item() ) );
		final var factory = scope.getSessionFactory();
		final var persister = factory.getMappingMetamodel().getEntityDescriptor( Item.class );
		final var loader = new SingleIdEntityLoaderStandardImpl<>( persister, new LoadQueryInfluencers( factory ) );
		final var dialect = (CountingDialect) factory.getJdbcServices().getDialect();
		final int initialTranslations = dialect.selectTranslations.get();
		for ( int round = 0; round < 3; round++ ) {
			for ( String tenant : new String[] { "mine", "yours", "root", "yours", "mine", "root" } ) {
				inTenant( scope, tenant, session -> {
					final var snapshot = loader.loadDatabaseSnapshot( 1L, session.unwrap( SessionImplementor.class ) );
					if ( tenant.equals( "yours" ) ) {
						assertNull( snapshot );
					}
					else {
						assertNotNull( snapshot );
						assertEquals( "mine", snapshot[persister.getPropertyIndex( "tenant" )] );
					}
				} );
			}
			assertEquals( 2, dialect.selectTranslations.get() - initialTranslations );
		}
	}

	/** Counts real translations while retaining H2's translator and database execution. */
	public static class CountingDialect extends H2Dialect {
		private final AtomicInteger selectTranslations = new AtomicInteger();

		@Override
		public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
			final var delegate = super.getSqlAstTranslatorFactory();
			return new SqlAstTranslatorFactory() {
				@Override
				public <S extends Statement, O extends JdbcOperation> SqlAstTranslator<O> buildTranslator(
						SqlAstTranslationRequest<S, O> request) {
					if ( request instanceof SqlAstTranslationRequest.Select ) {
						selectTranslations.incrementAndGet();
					}
					return delegate.buildTranslator( request );
				}
			};
		}
	}

	@Entity(name = "SnapshotPlanItem")
	static class Item {
		@Id Long id = 1L;
		@TenantId String tenant;
	}
}
