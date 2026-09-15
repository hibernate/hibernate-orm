/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.jdbc.Expectation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(annotatedClasses = { TenantIdCustomSqlCallableTest.Legacy.class, TenantIdCustomSqlCallableTest.WithTenant.class })
@SessionFactory
@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
@ServiceRegistry(settings = {
		@Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"),
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdCustomSqlCallableTest {
	@BeforeAll
	void createFunctions(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				for ( boolean withTenant : new boolean[] { false, true } ) {
					final String table = withTenant ? "tenant_callable_tenant" : "tenant_callable_legacy";
					final String tenantParameter = withTenant ? ", p_tenant text" : "";
					final String tenantPredicate = withTenant ? " and tenant=coalesce(p_tenant,tenant)" : "";
					statement.execute( "create function " + table
							+ "_update(p_name text, p_version integer, p_id bigint, p_old_version integer" + tenantParameter
							+ ") returns numeric language sql as $$ with changed as (update " + table
							+ " set item_name=p_name, version=p_version where id=p_id and version=p_old_version"
							+ tenantPredicate + " returning 1) select count(*)::numeric from changed $$" );
					statement.execute( "create function " + table + "_delete(p_id bigint, p_version integer" + tenantParameter
							+ ") returns numeric language sql as $$ with changed as (delete from " + table
							+ " where id=p_id and version=p_version" + tenantPredicate
							+ " returning 1) select count(*)::numeric from changed $$" );
				}
			}
		} ) );
	}

	@AfterAll
	void dropFunctions(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.doWork( connection -> {
			try ( var statement = connection.createStatement() ) {
				for ( boolean withTenant : new boolean[] { false, true } ) {
					final String table = withTenant ? "tenant_callable_tenant" : "tenant_callable_legacy";
					final String tenantParameter = withTenant ? ",text" : "";
					statement.execute( "drop function if exists " + table + "_update(text,integer,bigint,integer" + tenantParameter + ")" );
					statement.execute( "drop function if exists " + table + "_delete(bigint,integer" + tenantParameter + ")" );
				}
			}
		} ) );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	static Stream<Arguments> mutations() {
		return Stream.of( false, true ).flatMap( withTenant -> Stream.of( false, true ).flatMap( stateless ->
				Stream.of( "mine", "root" ).map( tenant -> Arguments.of( withTenant, stateless, tenant ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("mutations")
	void countsOutputParameter(boolean withTenant, boolean stateless, String tenant, SessionFactoryScope scope) {
		final TenantIdCustomSqlTest.VersionedBase item = withTenant ? new WithTenant() : new Legacy();
		scope.inTransaction( session -> session.persist( item ) );
		TenantIdCustomSqlTest.mutate( scope, tenant, stateless, item, false );
		scope.inTransaction( session -> {
			final var stored = session.find( item.getClass(), item.id );
			assertEquals( "changed", stored.name );
			assertEquals( "mine", stored.tenant );
			assertEquals( 1, stored.version );
		} );
		TenantIdCustomSqlTest.mutate( scope, tenant, stateless, item, true );
		scope.inTransaction( session -> assertNull( session.find( item.getClass(), item.id ) ) );
	}

	@Entity(name = "TenantCallableLegacy")
	@Table(name = "tenant_callable_legacy")
	@SQLUpdate(sql = "{?=call tenant_callable_legacy_update(?,?,?,?)}", callable = true, verify = Expectation.OutParameter.class)
	@SQLDelete(sql = "{?=call tenant_callable_legacy_delete(?,?)}", callable = true, verify = Expectation.OutParameter.class)
	static class Legacy extends TenantIdCustomSqlTest.VersionedBase {
	}

	@Entity(name = "TenantCallableWithTenant")
	@Table(name = "tenant_callable_tenant")
	@SQLUpdate(sql = "{?=call tenant_callable_tenant_update(?,?,?,?,?)}", callable = true, verify = Expectation.OutParameter.class)
	@SQLDelete(sql = "{?=call tenant_callable_tenant_delete(?,?,?)}", callable = true, verify = Expectation.OutParameter.class)
	static class WithTenant extends TenantIdCustomSqlTest.VersionedBase {
	}
}
