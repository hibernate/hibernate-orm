/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		DynamicInstantiationWithJoinAndGroupAndOrderByByTest.Item.class,
		DynamicInstantiationWithJoinAndGroupAndOrderByByTest.ItemSale.class
})
@JiraKey("HHH-15998")
public class DynamicInstantiationWithJoinAndGroupAndOrderByByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Item item1 = new Item();
			item1.setName( "Item 1" );
			session.persist( item1 );

			Item item2 = new Item();
			item2.setName( "Item 2" );
			session.persist( item2 );

			ItemSale itemSale11 = new ItemSale();
			itemSale11.setItem( item1 );
			itemSale11.setTotal( 1d );
			session.persist( itemSale11 );

			ItemSale itemSale12 = new ItemSale();
			itemSale12.setItem( item1 );
			itemSale12.setTotal( 2d );
			session.persist( itemSale12 );

			ItemSale itemSale21 = new ItemSale();
			itemSale21.setItem( item2 );
			itemSale21.setTotal( 5d );
			session.persist( itemSale21 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ItemSale" ).executeUpdate();
			session.createMutationQuery( "delete from Item" ).executeUpdate();
		} );
	}

	@Test
	public void testInstantiationGroupByAndOrderBy(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TypedQuery<Summary> query = session.createQuery(
					"select new " + getClass().getName() + "$Summary(i, sum(is.total))" +
							" from ItemSale is" +
							" join is.item i" +
							" group by i" +
							" order by i"
					,
					Summary.class
			);
			List<Summary> resultList = query.getResultList();
			assertEquals( 2, resultList.size() );
			assertEquals( "Item 1", resultList.get( 0 ).getItem().getName() );
			assertEquals( 3d, resultList.get( 0 ).getTotal() );
			assertEquals( "Item 2", resultList.get( 1 ).getItem().getName() );
			assertEquals( 5d, resultList.get( 1 ).getTotal() );
		} );
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ItemSale")
	public static class ItemSale {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne(optional = false)
		private Item item;

		private Double total;

		public Long getId() {
			return id;
		}

		public Item getItem() {
			return item;
		}

		public void setItem(Item item) {
			this.item = item;
		}

		public Double getTotal() {
			return total;
		}

		public void setTotal(Double total) {
			this.total = total;
		}
	}

	public static class Summary {
		private final Item item;

		private final Double total;

		private Summary(Item item, Double total) {
			this.item = item;
			this.total = total;
		}

		public Item getItem() {
			return item;
		}

		public Double getTotal() {
			return total;
		}
	}
}
