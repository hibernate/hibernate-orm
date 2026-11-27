package org.hibernate.temporal.audit;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Audited;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.audit.AuditLogFactory;
import org.hibernate.audit.ModificationType;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.generator.EventType.INSERT;
import static org.hibernate.generator.EventType.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@DomainModel(annotatedClasses = { AuditGeneratedValuesTest.Item.class, AuditGeneratedValuesTest.DynamicItem.class })
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = CHANGESET_ID_SUPPLIER,
		value = "org.hibernate.temporal.audit.AuditOneToManyIndexTest$ChangesetIds"))
class AuditGeneratedValuesTest {
	@Entity @Audited @Table(name = "audit_gen_item")
	@SecondaryTable(name = "audit_gen_detail")
	static class Item {
		@Id int id;
		int quantity;
		@Version @Generated(event = { INSERT, UPDATE }, sql = "current_timestamp") Instant version;
		@Generated(event = { INSERT, UPDATE }, sql = "random()") Double stamp;
		@Generated @ColumnDefault("random()") Double token;
		@CreationTimestamp Instant created;
		@Generated(event = { INSERT, UPDATE }, writable = true)
		@ColumnTransformer(write = "concat(?, '!')") String label = "item";
		@Generated(event = UPDATE, sql = "random()") Double updateStamp;
		@GeneratedColumn("quantity * 2") int doubled;
		@Column(table = "audit_gen_detail") String detail;
		@Generated @ColumnDefault("random()") @Column(table = "audit_gen_detail") Double detailToken;
	}

	private record Snapshot(int quantity, Double stamp, Double updateStamp, Double token,
			Instant created, String label, int doubled, Double detailToken) {
		Snapshot(Item item) {
			this( item.quantity, item.stamp, item.updateStamp, item.token, item.created, item.label, item.doubled, item.detailToken );
		}
	}

	@Test
	void revisionsMatchTheStoredRowAcrossFlushesUpdatesAndDeletion(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var item = new Item();
			item.id = 1;
			item.detail = "initial";
			session.persist( item );
			session.flush();
			item.quantity = 2;
		} );
		var first = scope.fromSession( session -> new Snapshot( session.find( Item.class, 1 ) ) );
		assertNotNull( first.stamp );
		assertNotNull( first.updateStamp );
		assertNotNull( first.token );
		assertNotNull( first.created );
		assertNotNull( first.detailToken );
		scope.inTransaction( session -> session.find( Item.class, 1 ).quantity = 3 );
		var second = scope.fromSession( session -> new Snapshot( session.find( Item.class, 1 ) ) );
		assertNotEquals( first.stamp, second.stamp );
		assertNotEquals( first.updateStamp, second.updateStamp );
		assertEquals( first.token, second.token );
		var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			var item = session.find( Item.class, 1 );
			observer.clear();
			session.remove( item );
		} );
		assertEquals( 0, selectCount( scope ) );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = log.getHistory( Item.class, 1 );
			// Core auditing excludes @Version, even when the database generates it.
			history.forEach( entry -> assertNull( entry.entity().version ) );
			assertEquals( List.of( ModificationType.ADD, ModificationType.MOD, ModificationType.DEL ),
					history.stream().map( entry -> entry.modificationType() ).toList() );
			assertEquals( List.of( first, second, second ),
					history.stream().map( entry -> new Snapshot( entry.entity() ) ).toList() );
		}
	}

	@Entity @Audited @DynamicInsert @Table(name = "audit_gen_dynamic")
	static class DynamicItem {
		@Id int id;
		int quantity;
		@Generated @ColumnDefault("random()") @Column(insertable = false, updatable = false) Double token;
		@Generated(event = { INSERT, UPDATE }, sql = "random()") @Audited.Excluded Double ignored;
	}

	@Test
	void detachedStatelessUpdateUpsertAndDeletePreserveTheInsertDefault(SessionFactoryScope scope) {
		var item = new DynamicItem();
		item.id = 2;
		scope.inStatelessTransaction( session -> session.insert( item ) );
		var token = item.token;
		assertNotNull( token );
		var detached = new DynamicItem();
		detached.id = 2;
		detached.quantity = 1;
		scope.inStatelessTransaction( session -> session.update( detached ) );
		scope.inStatelessTransaction( session -> session.upsert( detached ) );
		scope.getCollectingStatementObserver().clear();
		scope.inStatelessTransaction( session -> session.delete( detached ) );
		assertEquals( 1, selectCount( scope ) );
		scope.inSession( session -> assertEquals( List.of( token, token, token, token ),
				session.createNativeQuery( "select token from audit_gen_dynamic_AUD where id = 2 order by REV",
						Double.class ).getResultList() ) );
	}

	@Test
	void upsertOfANewRowAuditsTheGeneratedDefault(SessionFactoryScope scope) {
		var item = new DynamicItem();
		item.id = 7;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		var stored = scope.fromSession( session -> session.find( DynamicItem.class, 7 ).token );
		assertNotNull( stored );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = log.getHistory( DynamicItem.class, 7 );
			assertEquals( 1, history.size() );
			assertEquals( ModificationType.ADD, history.get( 0 ).modificationType() );
			assertEquals( stored, history.get( 0 ).entity().token );
		}
	}

	@Test
	void detachedDeleteCapturesTheInputsOfAComputedAuditColumn(SessionFactoryScope scope) {
		var item = new Item();
		item.id = 3;
		item.quantity = 7;
		scope.inStatelessTransaction( session -> session.insert( item ) );
		var snapshot = scope.fromSession( session -> new Snapshot( session.find( Item.class, 3 ) ) );
		assertEquals( 14, snapshot.doubled );
		var detached = new Item();
		detached.id = 3;
		assertNotNull( item.version );
		detached.version = item.version;
		scope.inStatelessTransaction( session -> session.delete( detached ) );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( List.of( snapshot, snapshot ), log.getHistory( Item.class, 3 ).stream()
					.map( entry -> new Snapshot( entry.entity() ) ).toList() );
		}
	}

	@Test
	void batchedUpdateAndDeleteUseTheirOwnAuditInsert(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( int id = 5; id <= 6; id++ ) {
				var item = new Item();
				item.id = id;
				item.detail = "detail";
				session.persist( item );
			}
		} );
		var deleted = scope.fromSession( session -> new Snapshot( session.find( Item.class, 6 ) ) );
		var observer = scope.getCollectingStatementObserver();
		scope.inTransaction( session -> {
			session.setJdbcBatchSize( 5 );
			session.find( Item.class, 5 ).quantity++;
			session.remove( session.find( Item.class, 6 ) );
			session.flush();
			observer.clear();
		} );
		assertEquals( 0, selectCount( scope ) );
		observer.assertQueries().filteredOn( sql -> sql.startsWith( "insert into audit_gen_item_AUD" ) )
				.hasSize( 2 ).anyMatch( sql -> sql.contains( ") select " ) )
				.anyMatch( sql -> sql.contains( " values " ) );
		var updated = scope.fromSession( session -> new Snapshot( session.find( Item.class, 5 ) ) );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( updated, new Snapshot( log.getHistory( Item.class, 5 ).get( 1 ).entity() ) );
			assertEquals( deleted, new Snapshot( log.getHistory( Item.class, 6 ).get( 1 ).entity() ) );
		}
	}

	@Test
	void deletionAfterAFlushedUpdateAuditsTheLastGeneratedValues(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			var item = new Item();
			item.id = 8;
			session.persist( item );
		} );
		var last = scope.fromTransaction( session -> {
			var item = session.find( Item.class, 8 );
			item.quantity++;
			session.flush();
			var snapshot = new Snapshot( item );
			session.remove( item );
			return snapshot;
		} );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			var history = log.getHistory( Item.class, 8 );
			assertEquals( 2, history.size() );
			assertEquals( ModificationType.DEL, history.get( 1 ).modificationType() );
			assertEquals( last, new Snapshot( history.get( 1 ).entity() ) );
		}
	}

	@Test
	void canceledStatelessInsertNeedsNoSnapshot(SessionFactoryScope scope) {
		var item = new DynamicItem();
		item.id = 4;
		scope.inStatelessTransaction( session -> {
			session.insert( item );
			scope.getCollectingStatementObserver().clear();
			session.delete( item );
		} );
		assertEquals( 0, selectCount( scope ) );
		try (var log = AuditLogFactory.create( scope.getSessionFactory() )) {
			assertEquals( List.of(), log.getHistory( DynamicItem.class, 4 ) );
		}
	}

	private static long selectCount(SessionFactoryScope scope) {
		return scope.getCollectingStatementObserver().getSqlQueries().stream()
				.filter( sql -> sql.startsWith( "select" ) ).count();
	}
}
