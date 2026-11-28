package org.hibernate.orm.test.stateless;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.jdbc.Expectation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-21289")
@DomainModel(annotatedClasses = UpsertUpdateOnlyColumnTest.Item.class)
@SessionFactory
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

	/// Custom SQL for the update means the upsert is executed as an update
	/// followed, if no row exists, by an insert, on every database.
	@Test
	@DomainModel(annotatedClasses = MarkedItem.class)
	@SessionFactory
	void updateRespectsPositionReservedByExpectation(SessionFactoryScope scope) {
		var item = new MarkedItem();
		item.id = 7;
		scope.inTransaction( session -> session.persist( item ) );
		item.quantity = 23;
		scope.inStatelessTransaction( session -> session.upsert( item ) );
		scope.inSession( session -> {
			var stored = session.find( MarkedItem.class, 7 );
			assertEquals( 42, stored.marker );
			assertEquals( 23, stored.quantity );
		} );
	}

	@Entity
	@Table(name = "upsert_with_marker")
	@SQLUpdate(sql = "update upsert_with_marker set marker = ?, quantity = ? where id = ?", verify = MarkerExpectation.class)
	static class MarkedItem {
		@Id int id;
		int quantity;
		@Column(insertable = false, updatable = false) Integer marker;
	}

	public static class MarkerExpectation extends Expectation.RowCount {
		@Override
		public int getNumberOfParametersUsed() {
			return 1;
		}

		@Override
		public int prepare(PreparedStatement statement) throws SQLException {
			statement.setInt( 1, 42 );
			return 1;
		}
	}
}
