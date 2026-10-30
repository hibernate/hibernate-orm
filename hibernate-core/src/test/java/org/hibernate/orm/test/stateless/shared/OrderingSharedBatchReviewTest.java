package org.hibernate.orm.test.stateless.shared;


import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.annotations.SecondaryRow;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = OrderingSharedBatchReviewTest.Entry.class)
@SessionFactory
public class OrderingSharedBatchReviewTest {
	@Test
	void singletonUpdateFollowsOrdinaryUpdate(SessionFactoryScope scope) {
		scope.inStatelessTransaction( session -> session.insert( new Entry( "initial" ) ) );
		scope.inTransaction( parent -> {
			parent.setJdbcBatchSize( 0 );
			try (var child = parent.statelessWithOptions().jdbcBatchSize( 5 ).connection().open()) {
				child.update( new Entry( "first" ) );
				child.updateMultiple( List.of( new Entry( "second" ) ) );
			}
		} );

		scope.inTransaction( session -> {
			var found = session.find( Entry.class, 1 );
			assertEquals( "second", found.label );
			assertEquals( "second", found.detail );
		} );
	}

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Entity(name = "OrderingReviewEntry")
	@Table(name = "review_entry")
	@SecondaryTable(name = "review_detail")
	@SecondaryRow(table = "review_detail", optional = false)
	public static class Entry {
		@Id
		int id;
		String label;
		@Column(table = "review_detail")
		String detail;

		public Entry() {}

		Entry(String value) {
			id = 1;
			label = value;
			detail = value;
		}
	}
}
