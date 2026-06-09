/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.multitenancy.rowsecurity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.TenantId;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.rowsecurity.CockroachRowLevelSecurity;
import org.hibernate.dialect.rowsecurity.DB2RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.OracleDeepDataSecurityRowLevelSecurity;
import org.hibernate.dialect.rowsecurity.PostgreSQLRowLevelSecurity;
import org.hibernate.dialect.rowsecurity.RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.SQLServerRowLevelSecurity;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;

@BaseUnitTest
class RowLevelSecurityTest {

	@Test
	void postgreSqlTenantIdRegistersRowLevelSecurityDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, PostgreSQLDialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( Document.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( Document.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			final String predicate = "tenant_id = cast(nullif(current_setting('hibernate.tenant_id', true), '') as uuid)"
					+ " or coalesce(cast(nullif(current_setting('hibernate.tenant_id_root', true), '') as boolean), false)";
			assertThat( commands ).containsExactly(
					"alter table document enable row level security",
					"alter table document force row level security",
					"create policy hibernate_tenant_isolation on document using (" + predicate + ")"
							+ " with check (" + predicate + ")"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void db2TenantIdRegistersRowLevelSecurityDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, DB2Dialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( StringDocument.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> auxiliaryCommands = metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
					.flatMap( object -> Arrays.stream( object.sqlCreateStrings( context ) ) )
					.toList();
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			assertThat( auxiliaryCommands ).containsExactly(
					"create or replace variable hibernate.tenant_id varchar(255)",
					"create or replace variable hibernate.tenant_id_root smallint default 0"
			);
			final String predicate = "tenant_id = cast(hibernate.tenant_id as varchar(255))"
					+ " or hibernate.tenant_id_root = 1";
			assertThat( commands ).containsExactly(
					"create or replace permission " + db2PermissionName( table ) + " on document"
							+ " for rows where " + predicate
							+ " enforced for all access enable",
					"alter table document activate row access control"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void rowLevelSecurityCanBeDisabled() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, DB2Dialect.class )
				.applySetting( MULTI_TENANT_RLS_ENABLED, false )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( StringDocument.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );

			assertThat( metadata.getDatabase().getAuxiliaryDatabaseObjects() ).isEmpty();
			assertThat( table.getInitCommands( context ) ).isEmpty();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void db2UuidTenantIdUsesBitFormatting() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, DB2Dialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( Document.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( Document.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			final String predicate = "tenant_id = varchar_bit_format(hibernate.tenant_id,"
					+ " 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx')"
					+ " or hibernate.tenant_id_root = 1";
			assertThat( commands ).containsExactly(
					"create or replace permission " + db2PermissionName( table ) + " on document"
							+ " for rows where " + predicate
							+ " enforced for all access enable",
					"alter table document activate row access control"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void oracleDeepDataSecurityTenantIdRegistersDataGrantDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, Oracle26DdsOrdersDialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( Order.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( Order.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> auxiliaryCommands = metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
					.flatMap( object -> Arrays.stream( object.sqlCreateStrings( context ) ) )
					.toList();
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			assertThat( auxiliaryCommands ).containsExactly(
					"create or replace end user context DEVELOPER.HIBERNATE_TENANCY using json schema "
							+ "'{\"type\":\"object\",\"properties\":{\"tenant_id\":{\"type\":[\"string\",\"null\"],\"default\":null},"
							+ "\"tenant_id_root\":{\"type\":\"string\",\"default\":\"false\"}}}'",
					"create or replace package " + oracleContextPackageName( "DEVELOPER.HIBERNATE_TENANCY" )
							+ " authid current_user as"
							+ " procedure set_tenant(p_tenant_id varchar2, p_tenant_id_root varchar2); end;",
					"create or replace package body " + oracleContextPackageName( "DEVELOPER.HIBERNATE_TENANCY" )
							+ " as procedure set_tenant(p_tenant_id varchar2, p_tenant_id_root varchar2) is"
							+ " begin execute immediate 'update sys.end_user_context t"
							+ " set t.context.tenant_id = :tenant_id, t.context.tenant_id_root = :tenant_id_root"
							+ " where owner = ''DEVELOPER'' and name = ''HIBERNATE_TENANCY'''"
							+ " using p_tenant_id, p_tenant_id_root; end; end;",
					"grant execute on " + oracleContextPackageName( "DEVELOPER.HIBERNATE_TENANCY" )
							+ " to database_role",
					"create or replace data grant " + oracleContextDataGrantName( "DEVELOPER.HIBERNATE_TENANCY" )
							+ " as select, update on sys.end_user_context"
							+ " where owner = 'DEVELOPER' and name = 'HIBERNATE_TENANCY'"
							+ " to orders_role"
			);
			final String predicate = "tenant_id = ORA_END_USER_CONTEXT.DEVELOPER.HIBERNATE_TENANCY.tenant_id"
					+ " or ORA_END_USER_CONTEXT.DEVELOPER.HIBERNATE_TENANCY.tenant_id_root = 'true'";
			assertThat( commands ).containsExactly(
					"create or replace data grant " + oracleDataGrantName( table )
							+ " as select, insert, update, delete on DEVELOPER.orders"
							+ " where " + predicate
							+ " to orders_role",
					"set use data grants only on DEVELOPER.orders enabled"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void oracleDeepDataSecurityIsOptIn() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, Oracle26Dialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( StringDocument.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );

			assertThat( metadata.getDatabase().getDialect().getRowLevelSecurity().supportsRowLevelSecurity() )
					.isFalse();
			assertThat( metadata.getDatabase().getAuxiliaryDatabaseObjects() ).isEmpty();
			assertThat( table.getInitCommands( context ) ).isEmpty();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void oracleDeepDataSecurityCanDisableUseDataGrantsOnlyDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, Oracle26DdsNoUseDataGrantsOnlyDialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( StringDocument.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			assertThat( commands ).hasSize( 1 );
			assertThat( commands.get( 0 ) )
					.startsWith( "create or replace data grant " + oracleDataGrantName( table ) );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void sqlServerTenantIdRegistersRowLevelSecurityDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, SQLServer2016Dialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( StringDocument.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
					.flatMap( object -> Arrays.stream( object.sqlCreateStrings( context ) ) )
					.toList();

			final String policyName = "dbo." + sqlServerObjectBaseName( table );
			final String functionName = policyName + "_predicate";
			final String predicate = "@tenant_id = cast(session_context(N'hibernate.tenant_id') as varchar(255))"
					+ " or cast(session_context(N'hibernate.tenant_id_root') as bit) = 1";
			assertThat( commands ).containsExactly(
					"create function " + functionName + "(@tenant_id varchar(255))"
							+ " returns table"
							+ " with schemabinding"
							+ " as"
							+ " return select 1 as hibernate_tenant_isolation_result"
							+ " where " + predicate,
					"create security policy " + policyName
							+ " add filter predicate " + functionName + "(tenant_id)"
							+ " on dbo.document,"
							+ " add block predicate " + functionName + "(tenant_id)"
							+ " on dbo.document after insert,"
							+ " add block predicate " + functionName + "(tenant_id)"
							+ " on dbo.document after update"
							+ " with (state = on)"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void spannerPostgreSqlDoesNotInheritPostgreSqlRowLevelSecurity() {
		assertThat( new SpannerPostgreSQLDialect().getRowLevelSecurity().supportsRowLevelSecurity() )
				.isFalse();
	}

	@Test
	void cockroachTenantIdRegistersRowLevelSecurityDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, Cockroach252Dialect.class )
				.build();
		try {
			final var metadata = new MetadataSources( registry )
					.addAnnotatedClass( Document.class )
					.buildMetadata();
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( Document.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			final String predicate = "tenant_id = cast(nullif(substring(current_setting('application_name', true)"
					+ " from '^hibernate_orm_rls:[^:]*:(.*)$'), '') as uuid)"
					+ " or split_part(current_setting('application_name', true), ':', 2) = 'true'";
			assertThat( commands ).containsExactly(
					"alter table document enable row level security",
					"alter table document force row level security",
					"create policy hibernate_tenant_isolation on document using (" + predicate + ")"
							+ " with check (" + predicate + ")"
			);
		}
		finally {
			StandardServiceRegistryBuilder.destroy( registry );
		}
	}

	@Test
	void cockroachRowLevelSecurityIsVersionGated() {
		assertThat( new CockroachDialect( DatabaseVersion.make( 25, 1 ) )
				.getRowLevelSecurity()
				.supportsRowLevelSecurity() )
				.isFalse();
		assertThat( new CockroachDialect( DatabaseVersion.make( 25, 2 ) )
				.getRowLevelSecurity()
				.supportsRowLevelSecurity() )
				.isTrue();
	}

	@Test
	void postgreSqlRowLevelSecurityUsesHibernateTenantSettings() {
		assertThat( PostgreSQLRowLevelSecurity.INSTANCE.getTenantIdentifierSettingName() )
				.isEqualTo( "hibernate.tenant_id" );
		assertThat( PostgreSQLRowLevelSecurity.INSTANCE.getRootTenantIdentifierSettingName() )
				.isEqualTo( "hibernate.tenant_id_root" );
	}

	@Test
	void cockroachRowLevelSecurityUsesApplicationNameSetting() {
		assertThat( CockroachRowLevelSecurity.INSTANCE.getTenantIdentifierSettingName() )
				.isEqualTo( "application_name" );
		assertThat( CockroachRowLevelSecurity.INSTANCE.getRootTenantIdentifierSettingName() )
				.isEqualTo( "application_name" );
	}

	@Test
	void sqlServerRowLevelSecurityUsesHibernateTenantSessionContextKeys() {
		assertThat( new SQLServerDialect().getRowLevelSecurity().supportsRowLevelSecurity() )
				.isFalse();
		assertThat( new SQLServer2016Dialect().getRowLevelSecurity().supportsRowLevelSecurity() )
				.isTrue();
		assertThat( SQLServerRowLevelSecurity.INSTANCE.getTenantIdentifierSettingName() )
				.isEqualTo( "hibernate.tenant_id" );
		assertThat( SQLServerRowLevelSecurity.INSTANCE.getRootTenantIdentifierSettingName() )
				.isEqualTo( "hibernate.tenant_id_root" );
	}

	@Test
	void db2RowLevelSecurityUsesHibernateTenantVariables() {
		assertThat( DB2RowLevelSecurity.INSTANCE.getTenantIdentifierSettingName() )
				.isEqualTo( "hibernate.tenant_id" );
		assertThat( DB2RowLevelSecurity.INSTANCE.getRootTenantIdentifierSettingName() )
				.isEqualTo( "hibernate.tenant_id_root" );
	}

	@Test
	void oracleDeepDataSecurityUsesConfiguredContextAttributes() {
		final var rowLevelSecurity = new OracleDeepDataSecurityRowLevelSecurity(
				"app.hibernate_tenancy",
				"tenant",
				"root_tenant",
				"employee_role",
				"database_role",
				true
		);

		assertThat( rowLevelSecurity.getTenantIdentifierSettingName() )
				.isEqualTo( "app.hibernate_tenancy.tenant" );
		assertThat( rowLevelSecurity.getRootTenantIdentifierSettingName() )
				.isEqualTo( "app.hibernate_tenancy.root_tenant" );
	}

	private static String db2PermissionName(org.hibernate.mapping.Table table) {
		return DB2RowLevelSecurity.TENANT_ISOLATION_PERMISSION + "_" + Integer.toUnsignedString(
				table.getQualifiedTableName().toString().hashCode(),
				36
		);
	}

	private static String sqlServerObjectBaseName(org.hibernate.mapping.Table table) {
		return SQLServerRowLevelSecurity.TENANT_ISOLATION_POLICY + "_" + Integer.toUnsignedString(
				table.getQualifiedTableName().toString().hashCode(),
				36
		);
	}

	private static String oracleDataGrantName(org.hibernate.mapping.Table table) {
		return OracleDeepDataSecurityRowLevelSecurity.TENANT_ISOLATION_DATA_GRANT + "_" + Integer.toUnsignedString(
				table.getQualifiedTableName().toString().hashCode(),
				36
		);
	}

	private static String oracleContextDataGrantName(String tenantContextName) {
		return "DEVELOPER." + OracleDeepDataSecurityRowLevelSecurity.CONTEXT_ACCESS_DATA_GRANT + "_"
				+ Integer.toUnsignedString( tenantContextName.hashCode(), 36 );
	}

	private static String oracleContextPackageName(String tenantContextName) {
		return "DEVELOPER." + OracleDeepDataSecurityRowLevelSecurity.CONTEXT_PACKAGE + "_"
				+ Integer.toUnsignedString( tenantContextName.hashCode(), 36 );
	}

	public static class SQLServer2016Dialect extends SQLServerDialect {
		public SQLServer2016Dialect() {
			super( DatabaseVersion.make( 13 ) );
		}
	}

	public static class Cockroach252Dialect extends CockroachDialect {
		public Cockroach252Dialect() {
			super( DatabaseVersion.make( 25, 2 ) );
		}
	}

	public static class Oracle26Dialect extends OracleDialect {
		public Oracle26Dialect() {
			super( DatabaseVersion.make( 26 ) );
		}
	}

	public static class Oracle26DdsOrdersDialect extends Oracle26Dialect {
		@Override
		public RowLevelSecurity getRowLevelSecurity() {
			return new OracleDeepDataSecurityRowLevelSecurity(
					"DEVELOPER.HIBERNATE_TENANCY",
					"tenant_id",
					"tenant_id_root",
					"orders_role",
					"database_role",
					true
			);
		}
	}

	public static class Oracle26DdsNoUseDataGrantsOnlyDialect extends Oracle26Dialect {
		@Override
		public RowLevelSecurity getRowLevelSecurity() {
			return new OracleDeepDataSecurityRowLevelSecurity(
					"app.hibernate_tenancy",
					"tenant_id",
					"tenant_id_root",
					"employee_role",
					"database_role",
					false
			);
		}
	}

	@Entity(name = "RlsDocument")
	@Table(name = "document")
	static class Document {
		@Id
		UUID id;

		@TenantId
		@Column(name = "tenant_id", nullable = false)
		UUID tenantId;

		String title;
	}

	@Entity(name = "RlsStringDocument")
	@Table(name = "document")
	static class StringDocument {
		@Id
		String id;

		@TenantId
		@Column(name = "tenant_id", nullable = false)
		String tenantId;

		String title;
	}

	@Entity(name = "RlsOrder")
	@Table(name = "orders", schema = "DEVELOPER")
	static class Order {
		@Id
		Long orderId;

		@TenantId
		@Column(name = "tenant_id", nullable = false)
		Long tenantId;

		String productName;
	}
}
