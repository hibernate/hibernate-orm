package org.hibernate.temporal;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;

import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.StateManagementSettings.CHANGESET_ID_SUPPLIER;
import static org.hibernate.cfg.StateManagementSettings.TEMPORAL_TABLE_STRATEGY;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21285")
@SessionFactory
@DomainModel(annotatedClasses = { TemporalGeneratedUpsertDefaultHistoryTest.Item.class,
		TemporalGeneratedUpsertDefaultHistoryTest.DynamicItem.class })
@ServiceRegistry(settings = {
		@Setting(name = TEMPORAL_TABLE_STRATEGY, value = "HISTORY_TABLE"),
		@Setting(name = CHANGESET_ID_SUPPLIER, value = TemporalGeneratedPropertyHistoryTest.SUPPLIER)
})
class TemporalGeneratedUpsertDefaultHistoryTest {
	@MappedSuperclass
	static class ItemWithDefault {
		@Id int id = 1;
		int quantity = 1;
		@Generated @ColumnDefault("42") Integer answer;
	}

	@Entity(name = "UpsertDefaultItem")
	@Temporal
	static class Item extends ItemWithDefault {}

	@Entity(name = "UpsertDynamicDefaultItem")
	@Temporal
	@DynamicUpdate
	static class DynamicItem extends ItemWithDefault {}

	@Test
	@SkipForDialect(dialectClass = SpannerDialect.class,
			reason = "The Spanner INSERT OR UPDATE binds null for the insert-only default column")
	void upsertWithInsertDefault(SessionFactoryScope scope) {
		assertUpsertWithInsertDefault( scope, new Item() );
	}

	@Test
	void dynamicUpsertWithInsertDefault(SessionFactoryScope scope) {
		assertUpsertWithInsertDefault( scope, new DynamicItem() );
	}

	private static void assertUpsertWithInsertDefault(SessionFactoryScope scope, ItemWithDefault item) {
		var factory = scope.getSessionFactory();
		var first = factory.fromStatelessTransaction( session -> {
			session.upsert( item );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		var type = item.getClass();
		scope.inSession( session -> assertEquals( 42, session.find( type, item.id ).answer ) );
		var second = factory.fromStatelessTransaction( session -> {
			item.answer = 23;
			session.upsert( item );
			return session.unwrap( SharedSessionContractImplementor.class ).getCurrentChangesetIdentifier();
		} );
		scope.inSession( session -> assertEquals( 23, session.find( type, item.id ).answer ) );
		try ( var before = factory.withOptions().atChangeset( first ).openSession();
				var after = factory.withOptions().atChangeset( second ).openSession() ) {
			assertEquals( 42, before.find( type, item.id ).answer );
			assertEquals( 23, after.find( type, item.id ).answer );
		}
	}
}
