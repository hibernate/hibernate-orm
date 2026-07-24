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
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.boot.model.naming.NamingHelper;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.rowsecurity.DB2RowLevelSecurity;
import org.hibernate.dialect.rowsecurity.SQLServerRowLevelSecurity;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.JdbcSettings.DIALECT;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CREDENTIALS_MAPPER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;

@BaseUnitTest
class RowLevelSecurityTest {

	@Test
	void postgreSqlTenantIdRegistersRowLevelSecurityDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( DIALECT, PostgreSQLDialect.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, Document.class );
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
	void postgreSqlTenantCredentialsMapperUsesCurrentUserInRlsDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( DIALECT, PostgreSQLDialect.class )
				.applySetting( MULTI_TENANT_CREDENTIALS_MAPPER, TenantCredentialsMapperImpl.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, StringDocument.class );
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			final String predicate = "tenant_id = cast(current_user as varchar(255))";
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
				.applySetting( DIALECT, DB2Dialect.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, StringDocument.class );
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
					"create or replace permission "
							+ db2PermissionName( table, context ) + " on document"
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
	void db2TenantCredentialsMapperUsesCurrentUserInRlsDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( DIALECT, DB2Dialect.class )
				.applySetting( MULTI_TENANT_CREDENTIALS_MAPPER, TenantCredentialsMapperImpl.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, StringDocument.class );
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			assertThat( metadata.getDatabase().getAuxiliaryDatabaseObjects() ).isEmpty();
			final String predicate = "tenant_id = cast(current_user as varchar(255))";
			assertThat( commands ).containsExactly(
					"create or replace permission "
							+ db2PermissionName( table, context ) + " on document"
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
				.applySetting( DIALECT, DB2Dialect.class )
				.applySetting( MULTI_TENANT_RLS_ENABLED, false )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, StringDocument.class );
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
				.applySetting( DIALECT, DB2Dialect.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, Document.class );
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
					"create or replace permission "
							+ db2PermissionName( table, context ) + " on document"
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
	void sqlServerTenantIdRegistersRowLevelSecurityDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( DIALECT, SQLServer2016Dialect.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, StringDocument.class );
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
					.flatMap( object -> Arrays.stream( object.sqlCreateStrings( context ) ) )
					.toList();

			final String policyName = "dbo." + sqlServerObjectBaseName( table, context );
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
	void sqlServerTenantCredentialsMapperUsesCurrentUserInRlsDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( DIALECT, SQLServer2016Dialect.class )
				.applySetting( MULTI_TENANT_CREDENTIALS_MAPPER, TenantCredentialsMapperImpl.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, StringDocument.class );
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = metadata.getDatabase().getAuxiliaryDatabaseObjects().stream()
					.flatMap( object -> Arrays.stream( object.sqlCreateStrings( context ) ) )
					.toList();

			final String policyName = "dbo." + sqlServerObjectBaseName( table, context );
			final String functionName = policyName + "_predicate";
			final String predicate = "@tenant_id = cast(current_user as varchar(255))";
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
				.applySetting( DIALECT, Cockroach252Dialect.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, Document.class );
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
	void cockroachTenantCredentialsMapperUsesCurrentUserInRlsDdl() {
		final StandardServiceRegistry registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( DIALECT, Cockroach252Dialect.class )
				.applySetting( MULTI_TENANT_CREDENTIALS_MAPPER, TenantCredentialsMapperImpl.class )
				.build();
		try {
			final var metadata = MetadataBuildingTestHelper.buildMetadata( registry, StringDocument.class );
			final org.hibernate.mapping.Table table =
					metadata.getEntityBinding( StringDocument.class.getName() ).getTable();
			final var context =
					SqlStringGenerationContextImpl.forTests( metadata.getDatabase().getJdbcEnvironment() );
			final List<String> commands = table.getInitCommands( context ).stream()
					.flatMap( command -> Arrays.stream( command.initCommands() ) )
					.toList();

			final String predicate = "tenant_id = cast(current_user as varchar(255))";
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

	private static String db2PermissionName(org.hibernate.mapping.Table table, SqlStringGenerationContext context) {
		return DB2RowLevelSecurity.TENANT_ISOLATION_PERMISSION + "_"
			+ NamingHelper.INSTANCE.hashedName( table.getQualifiedName( context ) );
	}

	private static String sqlServerObjectBaseName(org.hibernate.mapping.Table table, SqlStringGenerationContext context) {
		return SQLServerRowLevelSecurity.TENANT_ISOLATION_POLICY + "_"
			+ NamingHelper.INSTANCE.hashedName( table.getQualifiedName( context ) );
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

	public static class TenantCredentialsMapperImpl implements TenantCredentialsMapper<String> {
		@Override @NonNull
		public String user(@NonNull String tenantIdentifier) {
			return tenantIdentifier;
		}

		@Override @NonNull
		public String password(@NonNull String tenantIdentifier) {
			return tenantIdentifier;
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
