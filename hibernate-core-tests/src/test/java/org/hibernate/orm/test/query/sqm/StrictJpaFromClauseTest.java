package org.hibernate.orm.test.query.sqm;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * With strict JPA query compliance, each root after the first is treated as a
 * cross join of the first, and its joins follow it. A path from an outer query
 * in the from clause of a subquery is not such a root.
 */
@DomainModel(annotatedClasses = {StrictJpaFromClauseTest.Parent.class, StrictJpaFromClauseTest.Item.class})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true"))
@SessionFactory
@JiraKey("HHH-21128")
public class StrictJpaFromClauseTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Parent first = new Parent( 1 );
			final Parent second = new Parent( 2 );
			session.persist( first );
			session.persist( second );
			// the third parent has no items
			session.persist( new Parent( 3 ) );
			session.persist( new Item( 1, first ) );
			session.persist( new Item( 2, first ) );
			session.persist( new Item( 3, second ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			// roots after the first, with joins of their own
			"3 | select count(b) from Parent a, Item b where b.parent = a",
			"3 | select count(b) from Parent a, Item b join b.parent c where c.id = a.id",
			"3 | select count(b) from Parent a join a.items ai, Item b join b.parent c where c = a and b = ai",
			"3 | select count(b) from Parent a, Item b join b.parent c, Item d join d.parent e where c = a and d = b and e = a",
			// a path from the outer query in the from clause of a subquery
			"2 | select count(a) from Parent a where exists (select i from a.items i)",
			"2 | select count(a) from Parent a where exists (select i from a.items i, Item x where x = i)",
			"2 | select count(a) from Parent a where exists (select x from Item x, in(a.items) i where x = i)",
			"2 | select count(a) from Parent a where exists (select x from Item x join a.items i where x = i)"
	})
	public void count(long expected, String hql, SessionFactoryScope scope) {
		scope.inSession( session ->
				assertEquals( expected, session.createSelectionQuery( hql, Long.class ).getSingleResult() ) );
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		Integer id;
		@OneToMany(mappedBy = "parent")
		List<Item> items = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		Integer id;
		@ManyToOne
		Parent parent;

		public Item() {
		}

		public Item(Integer id, Parent parent) {
			this.id = id;
			this.parent = parent;
		}
	}
}
