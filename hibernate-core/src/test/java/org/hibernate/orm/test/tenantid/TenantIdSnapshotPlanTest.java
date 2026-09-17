/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.TenantId;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderStandardImpl;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.MutationStatement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperationQueryMutation;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.model.ast.TableMutation;
import org.hibernate.sql.model.jdbc.JdbcMutationOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.orm.test.tenantid.TenantIdMutationMappingTest.inTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialect(H2Dialect.class)
@DomainModel(annotatedClasses = TenantIdSnapshotPlanTest.Item.class)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = DIALECT, value = "org.hibernate.orm.test.tenantid.TenantIdSnapshotPlanTest$CountingDialect"),
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER, value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver")
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
				public SqlAstTranslator<JdbcSelect> buildSelectTranslator(
						SessionFactoryImplementor factory, SelectStatement statement) {
					selectTranslations.incrementAndGet();
					return delegate.buildSelectTranslator( factory, statement );
				}

				@Override
				public SqlAstTranslator<? extends JdbcOperationQueryMutation> buildMutationTranslator(
						SessionFactoryImplementor factory, MutationStatement statement) {
					return delegate.buildMutationTranslator( factory, statement );
				}

				@Override
				public <O extends JdbcMutationOperation> SqlAstTranslator<O> buildModelMutationTranslator(
						TableMutation<O> mutation, SessionFactoryImplementor factory) {
					return delegate.buildModelMutationTranslator( mutation, factory );
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
