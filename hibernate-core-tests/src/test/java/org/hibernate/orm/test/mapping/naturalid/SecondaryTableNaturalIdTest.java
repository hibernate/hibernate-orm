package org.hibernate.orm.test.mapping.naturalid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;

import org.hibernate.KeyType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Verifies natural-key loading qualifies a secondary-table natural ID with the correct table.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = SecondaryTableNaturalIdTest.Item.class)
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-15387")
public class SecondaryTableNaturalIdTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Item( 1, "first" ) );
			session.persist( new Item( 2, "second" ) );
		} );
	}

	@AfterAll
	void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SecondaryNaturalIdItem" ).executeUpdate() );
	}

	@Test
	void testFindByNaturalKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var observer = scope.getCollectingStatementObserver();
			observer.clear();

			final Item item = session.find( Item.class, "second", KeyType.NATURAL );
			assertNotNull( item );
			assertEquals( 2, item.id );
			assertEquals( "second", item.name );
			observer.assertStatements().hasSize( 1 );

			assertSame( item, session.find( Item.class, "second", KeyType.NATURAL ) );
			observer.assertStatements().hasSize( 1 );

			final Item first = session.find( Item.class, "first", KeyType.NATURAL );
			assertNotNull( first );
			assertEquals( 1, first.id );
			assertEquals( "first", first.name );
			assertNull( session.find( Item.class, "missing", KeyType.NATURAL ) );
		} );
	}

	@Entity(name = "SecondaryNaturalIdItem")
	@Table(name = "hhh15387_item")
	@SecondaryTable(name = "hhh15387_detail", pkJoinColumns = @PrimaryKeyJoinColumn(name = "item_id"))
	public static class Item {
		@Id
		Integer id;

		@NaturalId
		@Column(name = "name", table = "hhh15387_detail", nullable = false)
		String name;

		public Item() {
		}

		Item(int id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
