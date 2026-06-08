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
import org.hibernate.dialect.rowsecurity.SQLServerRowLevelSecurity;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE;
import static org.hibernate.cfg.DialectSpecificSettings.ORACLE_DEEP_DATA_SECURITY_USE_DATA_GRANTS_ONLY;
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
				.applySetting( AvailableSettings.DIALECT, Oracle26Dialect.class )
				.applySetting( ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME, "app.hibernate_tenancy" )
				.applySetting( ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE, "employee_role" )
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
					"create or replace end user context app.hibernate_tenancy using json schema "
							+ "'{\"type\":\"object\",\"properties\":{"
							+ "\"tenant_id\":{\"type\":\"string\"},"
							+ "\"tenant_id_root\":{\"type\":\"string\",\"default\":\"false\"}"
							+ "}}'"
			);
			final String predicate = "tenant_id = cast(ORA_END_USER_CONTEXT.app.hibernate_tenancy.tenant_id"
					+ " as varchar2(255 char))"
					+ " or ORA_END_USER_CONTEXT.app.hibernate_tenancy.tenant_id_root = 'true'";
			assertThat( commands ).containsExactly(
					"create or replace data grant " + oracleDataGrantName( table )
							+ " as select, insert, update, delete on document"
							+ " where " + predicate
							+ " to employee_role",
					"set use data grants only on document enabled"
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
				.applySetting( AvailableSettings.DIALECT, Oracle26Dialect.class )
				.applySetting( ORACLE_DEEP_DATA_SECURITY_TENANT_CONTEXT_NAME, "app.hibernate_tenancy" )
				.applySetting( ORACLE_DEEP_DATA_SECURITY_TENANT_DATA_GRANTEE, "employee_role" )
				.applySetting( ORACLE_DEEP_DATA_SECURITY_USE_DATA_GRANTS_ONLY, false )
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
}
