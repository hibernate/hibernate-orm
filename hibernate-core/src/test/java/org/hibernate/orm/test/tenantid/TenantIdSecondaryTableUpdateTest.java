/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.sql.SQLException;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.SecondaryRow;
import org.hibernate.annotations.TenantId;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
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
import static org.hibernate.orm.test.tenantid.TenantIdMutationMappingTest.inTenant;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		TenantIdSecondaryTableUpdateTest.Minimal.class, TenantIdSecondaryTableUpdateTest.Mutable.class,
		TenantIdSecondaryTableUpdateTest.Dynamic.class, TenantIdSecondaryTableUpdateTest.Versioned.class,
		TenantIdSecondaryTableUpdateTest.OptionalVersioned.class, TenantIdSecondaryTableUpdateTest.Excluded.class
})
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdSecondaryTableUpdateTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	static Stream<Arguments> updates() {
		return Stream.of( Minimal.class, Mutable.class, Dynamic.class, Versioned.class, OptionalVersioned.class, Excluded.class )
				.flatMap( type -> Stream.of( 0, 5 ).map( batchSize -> Arguments.of( type, batchSize ) ) );
	}

	@ParameterizedTest
	@MethodSource("updates")
	void rejectsChangedOwner(Class<? extends Item> type, int batchSize, SessionFactoryScope scope) {
		rejectsChangedOwner( type, batchSize, "original", "changed", scope );
	}

	private void rejectsChangedOwner(
			Class<? extends Item> type, int batchSize, String initial, String changed, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> {
			final var item = newItem( type );
			item.detail( initial );
			session.persist( item );
		} );
		try (var session = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession()) {
			session.beginTransaction();
			final var item = session.find( type, 1L );
			session.getTransaction().commit();
			// Keep the entity managed, but change ownership before the writing transaction.
			// Otherwise serializable databases may reject the transaction independently of the tenant check.
			inTenant( scope, "root", other -> other.createNativeMutationQuery(
					"update " + table( type ) + " set tenant='yours' where id=1" ).executeUpdate() );
			final var transaction = session.beginTransaction();
			try {
				session.setJdbcBatchSize( batchSize );
				item.detail( changed );
				assertThrows( PersistenceException.class, session::flush );
				// Check before rollback, which would otherwise hide an unauthorized update.
				session.doWork( connection -> {
					try (var statement = connection.createStatement();
						var rows = statement.executeQuery( "select detail from " + detailTable( type ) + " where id=1" )) {
						if ( initial == null ) {
							assertFalse( rows.next() );
						}
						else {
							assertTrue( rows.next() );
							assertEquals( initial, rows.getString( 1 ) );
						}
					}
				} );
			}
			finally {
				transaction.rollback();
			}
		}
		inTenant( scope, "yours", session -> assertEquals( initial, session.find( type, 1L ).detail() ) );
	}

	@ParameterizedTest
	@MethodSource("updates")
	void selectsOnlyWhenOwnerIsNotUpdated(Class<? extends Item> type, int batchSize, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> {
			session.persist( newItem( type ) );
			final var second = newItem( type );
			second.id = 2L;
			session.persist( second );
		} );
		inTenant( scope, "mine", session -> {
			session.setJdbcBatchSize( batchSize );
			final var first = session.find( type, 1L );
			final var second = session.find( type, 2L );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			first.detail( "changed" );
			second.detail( "changed" );
			session.flush();
			final boolean ownerUpdated = type == Versioned.class || type == OptionalVersioned.class
					|| type == Mutable.class && !legacy();
			assertEquals( ownerUpdated ? 0L : 2L,
					inspector.getSqlQueries().stream().filter( sql -> sql.startsWith( "select " ) ).count(),
					inspector.getSqlQueries().toString() );
		} );
		inTenant( scope, "mine", session -> {
			assertEquals( "changed", session.find( type, 1L ).detail() );
			assertEquals( "changed", session.find( type, 2L ).detail() );
		} );
	}

	protected boolean legacy() {
		return false;
	}

	static Stream<Arguments> optionalUpdates() {
		return Stream.of( Dynamic.class, OptionalVersioned.class, Excluded.class ).flatMap( type ->
				Stream.of( 0, 5 ).flatMap( batchSize -> Stream.of( false, true )
						.map( insert -> Arguments.of( type, batchSize, insert ) ) ) );
	}

	@ParameterizedTest
	@MethodSource("optionalUpdates")
	void optionalRowChangeChecksOwner(
			Class<? extends Item> type, int batchSize, boolean insert, SessionFactoryScope scope) {
		rejectsChangedOwner( type, batchSize, insert ? null : "original", insert ? "changed" : null, scope );
	}

	@ParameterizedTest
	@MethodSource("optionalUpdates")
	void optionalRowChangeSelectsOnlyWhenNeeded(
			Class<? extends Item> type, int batchSize, boolean insert, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> {
			final var item = newItem( type );
			item.detail( insert ? null : "original" );
			session.persist( item );
		} );
		inTenant( scope, "mine", session -> {
			session.setJdbcBatchSize( batchSize );
			final var item = session.find( type, 1L );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			item.detail( insert ? "changed" : null );
			session.flush();
			assertEquals( type == OptionalVersioned.class ? 0L : 1L,
					inspector.getSqlQueries().stream().filter( sql -> sql.startsWith( "select " ) ).count(),
					inspector.getSqlQueries().toString() );
		} );
		inTenant( scope, "mine", session -> assertEquals( insert ? "changed" : null, session.find( type, 1L ).detail() ) );
	}

	@ParameterizedTest
	@MethodSource("updates")
	void rootDoesNotSelectOwner(Class<? extends Item> type, int batchSize, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( newItem( type ) ) );
		inTenant( scope, "root", session -> {
			session.setJdbcBatchSize( batchSize );
			final var item = session.find( type, 1L );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			item.detail( "changed" );
			session.flush();
			assertTrue( inspector.getSqlQueries().stream().noneMatch( sql -> sql.startsWith( "select " ) ),
					inspector.getSqlQueries().toString() );
		} );
		inTenant( scope, "mine", session -> assertEquals( "changed", session.find( type, 1L ).detail() ) );
	}

	static Stream<Arguments> ownerUpdates() {
		return Stream.of( Mutable.class, Dynamic.class )
				.flatMap( type -> Stream.of( 0, 5 ).map( batchSize -> Arguments.of( type, batchSize ) ) );
	}

	@ParameterizedTest
	@MethodSource("ownerUpdates")
	void updatingOwnerDoesNotSelect(Class<? extends Item> type, int batchSize, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( newItem( type ) ) );
		inTenant( scope, "mine", session -> {
			session.setJdbcBatchSize( batchSize );
			final var item = session.find( type, 1L );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			if ( item instanceof Mutable mutable ) {
				mutable.name = "changed";
			}
			else {
				((Dynamic) item).name = "changed";
			}
			item.detail( "changed" );
			session.flush();
			assertTrue( inspector.getSqlQueries().stream().noneMatch( sql -> sql.startsWith( "select " ) ),
					inspector.getSqlQueries().toString() );
		} );
		inTenant( scope, "mine", session -> assertEquals( "changed", session.find( type, 1L ).detail() ) );
	}

	@Test
	void ownershipCheckIgnoresApplicationFilter(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Minimal() ) );
		inTenant( scope, "mine", session -> {
			final var item = session.find( Minimal.class, 1L );
			session.enableFilter( "hideSecondaryTenant" );
			item.detail( "changed" );
		} );
		inTenant( scope, "mine", session -> assertEquals( "changed", session.find( Minimal.class, 1L ).detail() ) );
	}

	@ParameterizedTest
	@ValueSource(classes = { Minimal.class, Versioned.class })
	@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
	void ownerRemainsLockedAfterFlush(Class<? extends Item> type, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( newItem( type ) ) );
		inTenant( scope, "mine", session -> {
			session.setJdbcBatchSize( 5 );
			session.find( type, 1L ).detail( "changed" );
			session.flush();
			final var exception = assertThrows( PersistenceException.class, () -> inTenant( scope, "root", other ->
					other.createNativeQuery( "select id from " + table( type ) + " where id=1 for update nowait", Long.class )
							.getSingleResult() ) );
			Throwable cause = exception;
			while ( cause != null && !(cause instanceof SQLException) ) {
				cause = cause.getCause();
			}
			assertEquals( "55P03", assertInstanceOf( SQLException.class, cause ).getSQLState() );
		} );
	}

	private static Item newItem(Class<? extends Item> type) {
		try {
			return type.getDeclaredConstructor().newInstance();
		}
		catch (ReflectiveOperationException e) {
			throw new AssertionError( e );
		}
	}

	private static String table(Class<?> type) {
		return type.getAnnotation( Table.class ).name();
	}

	private static String detailTable(Class<?> type) {
		return type.getAnnotation( SecondaryTable.class ).name();
	}

	@MappedSuperclass
	abstract static class Item {
		@Id Long id = 1L;
		@TenantId String tenant;
		abstract String detail();
		abstract void detail(String detail);
	}

	@Entity(name = "SecondaryTenantMinimal")
	@Table(name = "tenant_minimal")
	@SecondaryTable(name = "tenant_minimal_detail")
	@SecondaryRow(table = "tenant_minimal_detail", optional = false)
	@FilterDef(name = "hideSecondaryTenant", defaultCondition = "1=0")
	@Filter(name = "hideSecondaryTenant")
	static class Minimal extends Item {
		@Column(table = "tenant_minimal_detail") String detail = "original";
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}

	@Entity(name = "SecondaryTenantMutable")
	@Table(name = "tenant_mutable")
	@SecondaryTable(name = "tenant_mutable_detail")
	@SecondaryRow(table = "tenant_mutable_detail", optional = false)
	static class Mutable extends Item {
		String name = "original";
		@Column(table = "tenant_mutable_detail") String detail = "original";
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}

	@Entity(name = "SecondaryTenantDynamic")
	@Table(name = "tenant_dynamic")
	@SecondaryTable(name = "tenant_dynamic_detail_update")
	@DynamicUpdate
	static class Dynamic extends Item {
		String name = "original";
		@Column(table = "tenant_dynamic_detail_update") String detail = "original";
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}

	@Entity(name = "SecondaryTenantVersioned")
	@Table(name = "tenant_versioned")
	@SecondaryTable(name = "tenant_versioned_detail")
	@SecondaryRow(table = "tenant_versioned_detail", optional = false)
	static class Versioned extends Item {
		@Version int version;
		@Column(table = "tenant_versioned_detail") String detail = "original";
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}

	@Entity(name = "SecondaryTenantOptionalVersioned")
	@Table(name = "tenant_optional_versioned")
	@SecondaryTable(name = "tenant_optional_versioned_detail")
	static class OptionalVersioned extends Item {
		@Version int version;
		@Column(table = "tenant_optional_versioned_detail") String detail = "original";
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}

	@Entity(name = "SecondaryTenantExcluded")
	@Table(name = "tenant_excluded")
	@SecondaryTable(name = "tenant_excluded_detail")
	@DynamicUpdate
	static class Excluded extends Item {
		@Version int version;
		@OptimisticLock(excluded = true)
		@Column(table = "tenant_excluded_detail") String detail = "original";
		@Override String detail() { return detail; }
		@Override void detail(String detail) { this.detail = detail; }
	}
}
