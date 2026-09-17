/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.MappingException;
import org.hibernate.LockMode;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.TenantId;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.sql.ast.spi.model.MutatingTableReference;
import org.hibernate.sql.ast.spi.model.builder.TableDeleteBuilderStandard;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.TableMapping;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = { TenantIdCustomSqlTest.Legacy.class, TenantIdCustomSqlTest.WithTenant.class,
		TenantIdCustomSqlTest.VersionedLegacy.class, TenantIdCustomSqlTest.VersionedWithTenant.class,
		TenantIdCustomSqlTest.Generated.class, TenantIdCustomSqlTest.GeneratedWithoutPartition.class })
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdCustomSqlTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	enum Mapping {
		LEGACY(Legacy::new), TENANT(WithTenant::new), VERSIONED_LEGACY(VersionedLegacy::new), VERSIONED_TENANT(VersionedWithTenant::new);
		final Supplier<Base> constructor;

		Mapping(Supplier<Base> constructor) {
			this.constructor = constructor;
		}
	}

	static Stream<Arguments> mutations() {
		return Stream.of( Mapping.values() ).flatMap( mapping -> Stream.of( false, true ).flatMap( stateless ->
				Stream.of( "mine", "root" ).map( tenant -> Arguments.of( mapping, stateless, tenant ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("mutations")
	void acceptsOriginalOrTrailingTenantParameters(Mapping mapping, boolean stateless, String tenant, SessionFactoryScope scope) {
		final Base item = mapping.constructor.get();
		scope.inTransaction( session -> session.persist( item ) );
		mutate( scope, tenant, stateless, item, false );
		scope.inTransaction( session -> {
			final Base stored = session.find( item.getClass(), item.id );
			assertEquals( "changed", stored.name );
			assertEquals( "mine", stored.tenant );
			if ( stored instanceof VersionedBase versioned ) {
				assertEquals( 1, versioned.version );
			}
		} );
		mutate( scope, tenant, stateless, item, true );
		scope.inTransaction( session -> assertNull( session.find( item.getClass(), item.id ) ) );
	}

	static Stream<Arguments> foreignMutations() {
		return Stream.of( Mapping.TENANT, Mapping.VERSIONED_TENANT ).flatMap( mapping ->
				Stream.of( false, true ).map( delete -> Arguments.of( mapping, delete ) ) );
	}

	@ParameterizedTest
	@MethodSource("foreignMutations")
	void trailingTenantRestrictsForeignRows(Mapping mapping, boolean delete, SessionFactoryScope scope) {
		final Base item = mapping.constructor.get();
		scope.inTransaction( session -> session.persist( item ) );
		item.tenant = "yours";
		assertThrows( PersistenceException.class, () -> mutate( scope, "yours", true, item, delete ) );
		scope.inTransaction( session -> assertEquals( "original", session.find( item.getClass(), item.id ).name ) );
	}

	static Stream<Arguments> versionIncrements() {
		return Stream.of( Mapping.VERSIONED_LEGACY, Mapping.VERSIONED_TENANT ).flatMap( mapping ->
				Stream.of( false, true ).map( root -> Arguments.of( mapping, root ) ) );
	}

	static Stream<Arguments> staleVersions() {
		return Stream.of( Mapping.VERSIONED_LEGACY, Mapping.VERSIONED_TENANT ).flatMap( mapping ->
				Stream.of( false, true ).map( delete -> Arguments.of( mapping, delete ) ) );
	}

	@ParameterizedTest
	@MethodSource("staleVersions")
	void versionRestrictionIsRequiredWithOrWithoutTenantParameter(Mapping mapping, boolean delete, SessionFactoryScope scope) {
		final VersionedBase item = (VersionedBase) mapping.constructor.get();
		scope.inTransaction( session -> session.persist( item ) );
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"update " + item.getClass().getAnnotation( Table.class ).name() + " set version=version+1 where id=1" )
				.executeUpdate() );
		assertThrows( OptimisticLockException.class, () -> mutate( scope, "mine", true, item, delete ) );
		scope.inTransaction( session -> {
			final var stored = session.find( item.getClass(), item.id );
			assertEquals( "original", stored.name );
			assertEquals( 1, stored.version );
		} );
	}

	@ParameterizedTest
	@MethodSource("versionIncrements")
	void forcedVersionIncrementUsesGeneratedSql(Mapping mapping, boolean root, SessionFactoryScope scope) {
		final VersionedBase item = (VersionedBase) mapping.constructor.get();
		scope.inTransaction( session -> session.persist( item ) );
		try ( var session = scope.getSessionFactory().withOptions().tenantIdentifier( root ? "root" : "mine" ).openSession() ) {
			final var transaction = session.beginTransaction();
			final var managed = session.find( item.getClass(), item.id );
			session.lock( managed, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			transaction.commit();
		}
		scope.inTransaction( session -> {
			final var stored = session.find( item.getClass(), item.id );
			assertEquals( "original", stored.name );
			assertEquals( 1, stored.version );
		} );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void generatedSqlPlacesTenantLast(boolean stateless, SessionFactoryScope scope) {
		final Base item = stateless ? new GeneratedWithoutPartition() : new Generated();
		scope.inTransaction( session -> session.persist( item ) );
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		mutate( scope, "mine", stateless, item, false );
		assertTenantLast( inspector.getSqlQueries().stream().filter( sql -> sql.startsWith( "update " ) ).findFirst().orElseThrow() );
		inspector.clear();
		mutate( scope, "mine", stateless, item, true );
		assertTenantLast( inspector.getSqlQueries().stream().filter( sql -> sql.startsWith( "delete " ) ).findFirst().orElseThrow() );
	}

	private static void assertTenantLast(String sql) {
		assertTrue( sql.matches( ".*tenant=coalesce\\(\\?,(?:\\w+\\.)?tenant\\)" ), sql );
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 1, 4 })
	void invalidUpdateParameterCountIsRejected(int count, SessionFactoryScope scope) {
		final var factory = scope.getSessionFactory();
		final var persister = factory.getMappingMetamodel().getEntityDescriptor( Legacy.class );
		final var table = persister.getIdentifierTableMapping();
		final var details = new TableMapping.MutationDetails( MutationType.UPDATE, new Expectation.RowCount(),
				"call wrong_count(" + String.join( ",", java.util.Collections.nCopies( count, "?" ) ) + ")", false );
		final var builder = new TableUpdateBuilderStandard<MutationOperation>( persister,
				new MutatingTableReference( table ), details, null, factory );
		builder.addValueColumn( persister.findAttributeMapping( "name" ).getSelectable( 0 ) );
		builder.addKeyRestrictionsLeniently( table.getKeyMapping() );
		TenantIdHelper.applyTenantRestriction( persister, builder );
		final var exception = assertThrows( MappingException.class, builder::buildMutation );
		assertTrue( exception.getMessage().contains( "has " + count + " JDBC parameters" ), exception.getMessage() );
		assertTrue( exception.getMessage().contains( "expected 2 without the tenant id or 3" ), exception.getMessage() );
	}

	@Test
	void invalidDeleteParameterCountIsRejected(SessionFactoryScope scope) {
		final var factory = scope.getSessionFactory();
		final var persister = factory.getMappingMetamodel().getEntityDescriptor( Legacy.class );
		final var table = persister.getIdentifierTableMapping();
		final var details = new TableMapping.MutationDetails( MutationType.DELETE, new Expectation.RowCount(),
				"delete from tenant_custom_legacy where id=? and tenant=? and item_name=?", false );
		final var builder = new TableDeleteBuilderStandard( persister, new MutatingTableReference( table ), details, null, factory );
		builder.addKeyRestrictionsLeniently( table.getKeyMapping() );
		TenantIdHelper.applyTenantRestriction( persister, builder );
		final var exception = assertThrows( MappingException.class, builder::buildMutation );
		assertTrue( exception.getMessage().contains( "expected 1 without the tenant id or 2" ), exception.getMessage() );
	}

	static void mutate(SessionFactoryScope scope, String tenant, boolean stateless, Base item, boolean delete) {
		if ( stateless ) {
			try ( var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				try {
					if ( delete ) {
						session.delete( item );
					}
					else {
						item.name = "changed";
						session.update( item );
					}
					transaction.commit();
				}
				finally {
					if ( transaction.isActive() ) {
						transaction.rollback();
					}
				}
			}
		}
		else {
			try ( var session = scope.getSessionFactory().withOptions().tenantIdentifier( tenant ).openSession() ) {
				final var transaction = session.beginTransaction();
				try {
					final Base managed = session.find( item.getClass(), item.id );
					if ( delete ) {
						session.remove( managed );
					}
					else {
						managed.name = "changed";
					}
					transaction.commit();
				}
				finally {
					if ( transaction.isActive() ) {
						transaction.rollback();
					}
				}
			}
		}
	}

	@MappedSuperclass
	static class Base {
		@Id Long id = 1L;
		@TenantId String tenant;
		@Column(name = "item_name") String name = "original";
	}

	@MappedSuperclass
	static class VersionedBase extends Base {
		@Version int version;
	}

	@Entity(name = "TenantCustomLegacy")
	@Table(name = "tenant_custom_legacy")
	@SQLUpdate(sql = "update tenant_custom_legacy set item_name=? where id=? and '?'='?' /* ? */", verify = Expectation.RowCount.class)
	@SQLDelete(sql = "delete from tenant_custom_legacy where id=? -- ?\n", verify = Expectation.RowCount.class)
	static class Legacy extends Base {
	}

	@Entity(name = "TenantCustomWithTenant")
	@Table(name = "tenant_custom_tenant")
	@SQLUpdate(sql = "update tenant_custom_tenant set item_name=? where id=? and tenant=coalesce(?,tenant)", verify = Expectation.RowCount.class)
	@SQLDelete(sql = "delete from tenant_custom_tenant where id=? and tenant=coalesce(?,tenant)", verify = Expectation.RowCount.class)
	static class WithTenant extends Base {
	}

	@Entity(name = "TenantCustomVersionedLegacy")
	@Table(name = "tenant_custom_version_legacy")
	@SQLUpdate(sql = "update tenant_custom_version_legacy set item_name=?,version=? where id=? and version=?", verify = Expectation.RowCount.class)
	@SQLDelete(sql = "delete from tenant_custom_version_legacy where id=? and version=?", verify = Expectation.RowCount.class)
	static class VersionedLegacy extends VersionedBase {
	}

	@Entity(name = "TenantCustomVersionedTenant")
	@Table(name = "tenant_custom_version_tenant")
	@SQLUpdate(sql = "update tenant_custom_version_tenant set item_name=?,version=? where id=? and version=? and tenant=coalesce(?,tenant)", verify = Expectation.RowCount.class)
	@SQLDelete(sql = "delete from tenant_custom_version_tenant where id=? and version=? and tenant=coalesce(?,tenant)", verify = Expectation.RowCount.class)
	static class VersionedWithTenant extends VersionedBase {
	}

	@Entity(name = "TenantCustomGenerated")
	@Table(name = "tenant_custom_generated")
	@DynamicUpdate
	static class Generated extends VersionedBase {
		@PartitionKey @Column(updatable = false) String partitionKey = "partition";
	}

	@Entity(name = "TenantCustomGeneratedWithoutPartition")
	@Table(name = "tenant_custom_generated_no_partition")
	static class GeneratedWithoutPartition extends VersionedBase {
	}
}
