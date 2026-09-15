/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.tenantid;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.HQLSelect;
import org.hibernate.annotations.TenantId;
import org.hibernate.dialect.sql.ast.internal.SpannerSqlAstTranslator;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.internal.TenantIdHelper;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.jdbc.mutation.internal.MutationQueryOptions;
import org.hibernate.sql.ast.spi.model.builder.TableUpdateBuilderStandard;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER;
import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_RLS_ENABLED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {
		TenantIdMutationMappingTest.Joined.class, TenantIdMutationMappingTest.JoinedSub.class,
		TenantIdMutationMappingTest.Union.class, TenantIdMutationMappingTest.UnionSub.class,
		TenantIdMutationMappingTest.DynamicItem.class, TenantIdMutationMappingTest.Provided.class,
		TenantIdMutationMappingTest.Empty.class, TenantIdMutationMappingTest.Versioned.class
})
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(settings = {
		@Setting(name = MULTI_TENANT_IDENTIFIER_RESOLVER,
				value = "org.hibernate.orm.test.tenantid.TenantIdMutationTest$Resolver"),
		@Setting(name = MULTI_TENANT_RLS_ENABLED, value = "false")
})
class TenantIdMutationMappingTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@ParameterizedTest
	@ValueSource(classes = { JoinedSub.class, UnionSub.class, Provided.class })
	void statelessUpdate(Class<? extends Item> type, SessionFactoryScope scope) throws Exception {
		final var item = type.getDeclaredConstructor().newInstance();
		inTenant( scope, "mine", session -> session.persist( item ) );
		item.change();
		inStatelessTenant( scope, "mine", session -> session.update( item ) );
		inTenant( scope, "mine", session -> assertEquals( "updated", session.find( type, 1L ).value() ) );
	}

	@ParameterizedTest
	@ValueSource(classes = { JoinedSub.class, UnionSub.class, Provided.class })
	void statelessDelete(Class<? extends Item> type, SessionFactoryScope scope) throws Exception {
		final var item = type.getDeclaredConstructor().newInstance();
		inTenant( scope, "mine", session -> session.persist( item ) );
		inStatelessTenant( scope, "mine", session -> session.delete( item ) );
		inTenant( scope, "root", session -> assertNull( session.find( type, 1L ) ) );
	}

	@ParameterizedTest
	@ValueSource(classes = { JoinedSub.class, UnionSub.class, Provided.class })
	void statelessUpsert(Class<? extends Item> type, SessionFactoryScope scope) throws Exception {
		final var item = type.getDeclaredConstructor().newInstance();
		inStatelessTenant( scope, "mine", session -> session.upsert( item ) );
		assertEquals( "mine", item.tenant );
		item.change();
		inStatelessTenant( scope, "mine", session -> session.upsert( item ) );
		inTenant( scope, "mine", session -> assertEquals( "updated", session.find( type, 1L ).value() ) );
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "root" })
	void upsertWithoutUpdatableAttributes(String tenant, SessionFactoryScope scope) {
		final var item = new Empty();
		inStatelessTenant( scope, "mine", session -> session.upsert( item ) );
		inStatelessTenant( scope, tenant, session -> session.upsert( item ) );
		inTenant( scope, "mine", session -> assertEquals( "mine", session.find( Empty.class, 1L ).tenant ) );
	}

	@Test
	@RequiresDialect(SpannerDialect.class)
	@RequiresDialect(SpannerPostgreSQLDialect.class)
	void upsertWithoutUpdatableAttributesChecksStoredTenant(SessionFactoryScope scope) {
		final var item = new Empty();
		inStatelessTenant( scope, "mine", session -> session.upsert( item ) );
		item.tenant = "yours";
		final var inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		inStatelessTenant( scope, "yours", session -> session.upsert( item ) );
		if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SpannerDialect ) {
			// GoogleSQL ignores the insert because the key already exists.
			inspector.assertExecutedCount( 1 );
			final var sql = inspector.getSqlQueries().get( 0 );
			assertTrue( sql.startsWith( "insert or ignore into " ), sql );
		}
		else {
			// The tenant restriction prevents the update from matching the existing row.
			// The fallback insert leaves that row untouched because its key already exists.
			inspector.assertExecutedCount( 2 );
			inspector.assertIsUpdate( 0 );
			inspector.assertIsInsert( 1 );
		}
		inTenant( scope, "mine", session -> assertEquals( "mine", session.find( Empty.class, 1L ).tenant ) );
		inTenant( scope, "yours", session -> assertNull( session.find( Empty.class, 1L ) ) );
	}

	@Test
	void dynamicUpdateOfSecondaryTable(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new DynamicItem() ) );
		inTenant( scope, "mine", session -> session.find( DynamicItem.class, 1L ).change() );
		inTenant( scope, "mine", session -> assertEquals( "updated", session.find( DynamicItem.class, 1L ).value() ) );
	}

	@Test
	void ownershipIgnoresApplicationFilter(SessionFactoryScope scope) {
		final var item = new DynamicItem();
		inTenant( scope, "mine", session -> session.persist( item ) );
		inTenant( scope, "mine", session -> {
			session.enableFilter( "hideMutationItems" );
			session.remove( item );
			assertTrue( session.getEnabledFilter( "hideMutationItems" ) != null );
		} );
		inTenant( scope, "root", session -> assertNull( session.find( DynamicItem.class, 1L ) ) );
	}

	@Test
	@RequiresDialect(value = PostgreSQLDialect.class, matchSubTypes = false)
	void ownershipLocksTenantTable(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new JoinedSub() ) );
		inStatelessTenant( scope, "mine", session -> {
			final var persister = scope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( JoinedSub.class );
			TenantIdHelper.checkTenantId( 1L, persister, (SharedSessionContractImplementor) session, false );
			final var exception = assertThrows( PersistenceException.class, () -> inTenant( scope, "root", other ->
					other.createNativeQuery( "select id from TenantJoined where id=1 for update nowait", Long.class )
							.getSingleResult() ) );
			Throwable cause = exception;
			while ( cause != null && !(cause instanceof SQLException) ) {
				cause = cause.getCause();
			}
			assertEquals( "55P03", assertInstanceOf( SQLException.class, cause ).getSQLState() );
		} );
	}

	@ParameterizedTest
	@ValueSource(classes = { Provided.class, DynamicItem.class })
	void removeDetachedOwnRow(Class<? extends Item> type, SessionFactoryScope scope) throws Exception {
		final var item = type.getDeclaredConstructor().newInstance();
		inTenant( scope, "mine", session -> session.persist( item ) );
		inTenant( scope, "mine", session -> session.remove( item ) );
		inTenant( scope, "root", session -> assertNull( session.find( type, 1L ) ) );
	}

	@ParameterizedTest
	@ValueSource(classes = { Provided.class, DynamicItem.class })
	void removeDetachedForeignRow(Class<? extends Item> type, SessionFactoryScope scope) throws Exception {
		final var item = type.getDeclaredConstructor().newInstance();
		inTenant( scope, "yours", session -> session.persist( item ) );
		item.tenant = "mine";
		assertThrows( PersistenceException.class, () -> inTenant( scope, "mine", session -> session.remove( item ) ) );
		inTenant( scope, "root", session -> assertEquals( "yours", session.find( type, 1L ).tenant ) );
	}

	@Test
	void tablePerClassUpdateChecksStoredTenant(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new UnionSub() ) );
		try (var session = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession()) {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( UnionSub.class, 1L );
				replace( scope, UnionSub.class, new UnionSub() );
				item.change();
				assertThrows( PersistenceException.class, session::flush );
			}
			finally {
				transaction.rollback();
			}
		}
		inTenant( scope, "yours", session -> assertEquals( "original", session.find( UnionSub.class, 1L ).value() ) );
	}

	@Test
	void collectionVersionUpdateChecksStoredTenant(SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Versioned() ) );
		try (var session = scope.getSessionFactory().withOptions().tenantIdentifier( "mine" ).openSession()) {
			final var transaction = session.beginTransaction();
			try {
				final var item = session.find( Versioned.class, 1L );
				assertTrue( item.tags.isEmpty() );
				replace( scope, Versioned.class, new Versioned() );
				item.tags.add( "updated" );
				assertThrows( PersistenceException.class, session::flush );
			}
			finally {
				transaction.rollback();
			}
		}
		inTenant( scope, "yours", session -> {
			final var item = session.find( Versioned.class, 1L );
			assertEquals( 0, item.version );
			assertTrue( item.tags.isEmpty() );
		} );
	}

	@ParameterizedTest
	@ValueSource(strings = { "mine", "root" })
	void collectionVersionUpdate(String tenant, SessionFactoryScope scope) {
		inTenant( scope, "mine", session -> session.persist( new Versioned() ) );
		inTenant( scope, tenant, session -> session.find( Versioned.class, 1L ).tags.add( "updated" ) );
		inTenant( scope, "mine", session -> assertEquals( 1, session.find( Versioned.class, 1L ).version ) );
	}

	@Test
	void spannerTenantRestriction(SessionFactoryScope scope) {
		final var factory = scope.getSessionFactory();
		final var persister = factory.getMappingMetamodel().getEntityDescriptor( UnionSub.class );
		final var builder = new TableUpdateBuilderStandard<MutationOperation>(
				persister, persister.getIdentifierTableMapping(), factory );
		builder.addValueColumn( persister.findAttributeMapping( "detail" ).getSelectable( 0 ) );
		builder.addKeyRestrictionsLeniently( persister.getIdentifierTableMapping().getKeyMapping() );
		TenantIdHelper.applyTenantRestriction( persister, builder );
		final var translator = new SpannerSqlAstTranslator<>(
				new SqlAstTranslationRequest.ModelMutation<>( factory, builder.buildMutation() ) );
		final String sql = translator.translate( null, MutationQueryOptions.INSTANCE ).getSqlString();
		assertTrue( sql.matches( ".*([a-z0-9_]+)\\.tenant=coalesce\\(\\?,\\1\\.tenant\\).*" ), sql );
	}

	static void inTenant(SessionFactoryScope scope, String tenant, Consumer<Session> action) {
		scope.inTransaction( factory -> factory.withOptions().tenantIdentifier( tenant ).openSession(), action::accept );
	}

	static void inStatelessTenant(SessionFactoryScope scope, String tenant, Consumer<StatelessSession> action) {
		try (var session = scope.getSessionFactory().withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession()) {
			final var transaction = session.beginTransaction();
			try {
				action.accept( session );
				transaction.commit();
			}
			finally {
				if ( transaction.isActive() ) {
					transaction.rollback();
				}
			}
		}
	}

	private static <T> void replace(SessionFactoryScope scope, Class<T> type, T replacement) {
		inTenant( scope, "root", session -> session.remove( session.find( type, 1L ) ) );
		inTenant( scope, "yours", session -> session.persist( replacement ) );
	}

	@MappedSuperclass
	abstract static class Item {
		@Id Long id = 1L;
		@TenantId String tenant;
		abstract void change();
		abstract String value();
	}

	@Entity(name = "TenantJoined")
	@Inheritance(strategy = InheritanceType.JOINED)
	abstract static class Joined extends Item {
	}

	@Entity(name = "TenantJoinedSub")
	@PrimaryKeyJoinColumn(name = "sub_id")
	static class JoinedSub extends Joined {
		String detail = "original";
		@Override void change() { detail = "updated"; }
		@Override String value() { return detail; }
	}

	@Entity(name = "TenantUnion")
	@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
	static class Union extends Item {
		String name = "original";
		@Override void change() { name = "updated"; }
		@Override String value() { return name; }
	}

	@Entity(name = "TenantUnionSub")
	@Table(name = "tenant_union_sub")
	static class UnionSub extends Union {
		String detail = "original";
		@Override void change() { detail = "updated"; }
		@Override String value() { return detail; }
	}

	@Entity(name = "TenantDynamicItem")
	@SecondaryTable(name = "tenant_dynamic_detail")
	@DynamicUpdate
	@FilterDef(name = "hideMutationItems", defaultCondition = "1=0")
	@Filter(name = "hideMutationItems")
	static class DynamicItem extends Item {
		String name = "original";
		@Column(table = "tenant_dynamic_detail") String detail = "original";
		@Override void change() { detail = "updated"; }
		@Override String value() { return detail; }
	}

	@Entity(name = "TenantProvidedItem")
	@HQLSelect(query = "from TenantProvidedItem where id=?1")
	static class Provided extends Item {
		String name = "original";
		@Override void change() { name = "updated"; }
		@Override String value() { return name; }
	}

	@Entity(name = "TenantEmptyItem")
	static class Empty {
		@Id Long id = 1L;
		@TenantId String tenant;
	}

	@Entity(name = "TenantVersionedCollection")
	static class Versioned {
		@Id Long id = 1L;
		@TenantId String tenant;
		@Version int version;
		@ElementCollection List<String> tags = new ArrayList<>();
	}
}
