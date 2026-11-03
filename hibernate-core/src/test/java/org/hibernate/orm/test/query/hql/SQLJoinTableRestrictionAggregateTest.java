package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-18996")
@DomainModel(annotatedClasses = {
		SQLJoinTableRestrictionAggregateTest.Cart.class,
		SQLJoinTableRestrictionAggregateTest.Item.class
})
@SessionFactory
class SQLJoinTableRestrictionAggregateTest {
	@Test
	void joinTableRestrictionIsAppliedToCount(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createNativeQuery( "alter table cart_items add active boolean default true not null" )
					.executeUpdate();

			final Cart cart = new Cart();
			cart.id = 1;
			final Item activeItem = new Item();
			activeItem.id = 1;
			final Item inactiveItem = new Item();
			inactiveItem.id = 2;

			cart.items.add( activeItem );
			cart.items.add( inactiveItem );
			session.persist( cart );
			session.persist( activeItem );
			session.persist( inactiveItem );

			session.flush();

			session.createNativeQuery( "update cart_items set active = false where item_id = 2" ).executeUpdate();
		} );

		final long count = scope.fromTransaction( session -> session.createQuery(
				"select count(item) from Cart cart join cart.items item where cart.id = :id",
				Long.class
		).setParameter( "id", 1 ).getSingleResult() );

		assertEquals( 1L, count );
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		public long id;
	}

	@Entity(name = "Cart")
	public static class Cart {
		@Id
		public long id;

		@OneToMany
		@JoinTable(
				name = "cart_items",
				joinColumns = @JoinColumn(name = "cart_id"),
				inverseJoinColumns = @JoinColumn(name = "item_id")
		)
		@SQLJoinTableRestriction("active = true")
		public Set<Item> items = new HashSet<>();
	}
}
