package org.hibernate.orm.test.stateless;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21289")
@DomainModel(annotatedClasses = UpsertUpdateOnlyColumnTest.Item.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
@RequiresDialect(MySQLDialect.class)
@RequiresDialect(SpannerDialect.class)
class UpsertUpdateOnlyColumnTest {
	@Entity
	@Table(name = "upsert_update_only_item")
	static class Item {
		@Id int id;
		int quantity;
		@Column(insertable = false) @ColumnDefault("42") Integer answer;
		@Generated @ColumnDefault("12") Integer initialScore;
		@Generated @ColumnDefault("7") @Column(updatable = false) Integer fixedScore;
	}

	@Test
	void updateOnlyColumnUsesDefaultOnInsertAndArgumentOnUpdate(SessionFactoryScope scope) {
		var item = new Item();
		item.id = 1;
		item.quantity = 3;
		item.answer = 99;
		item.initialScore = 88;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		scope.inSession( session -> {
			var stored = session.find( Item.class, 1 );
			assertEquals( 42, stored.answer );
			assertEquals( 12, stored.initialScore );
			assertEquals( 7, stored.fixedScore );
		} );
		item.answer = 23;
		item.initialScore = 34;
		item.fixedScore = 56;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		scope.inSession( session -> {
			var stored = session.find( Item.class, 1 );
			assertEquals( 23, stored.answer );
			assertEquals( 34, stored.initialScore );
			assertEquals( 7, stored.fixedScore );
		} );
	}
}
