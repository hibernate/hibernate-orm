/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.testing.orm.junit.ImplicitListAsBagProvider;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsBagProvider.class )
)
@DomainModel(
		annotatedClasses = {
				OneToManyEmptyCollectionTest.Order.class,
				OneToManyEmptyCollectionTest.Item.class
		}
)
@SessionFactory(generateStatistics = true)
public class OneToManyEmptyCollectionTest {

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Order order = new Order();
					session.persist( order );
				}
		);

		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					Order order = session.createQuery( "from Order", Order.class ).uniqueResult();
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
					List<Item> items = order.getEagerItems();
					assertThat( items.size(), is( 0 ) );
					assertThat( items, instanceOf( PersistentBag.class ) );
					assertThat( items, instanceOf( PersistentBag.class ) );

					PersistentBag bag = (PersistentBag) items;
					assertThat( bag.getOwner(), sameInstance( order ) );
					assertThat( bag.getKey(), is( 1L ) );
					assertThat(
							bag.getRole(),
							is( Order.class.getName() + ".eagerItems" )
					);
					assertTrue( bag.wasInitialized() );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );


					List<Item> lazyItems = order.getLazyItems();
					assertThat( lazyItems, instanceOf( PersistentBag.class ) );
					assertThat( lazyItems, instanceOf( PersistentBag.class ) );

					PersistentBag lazyBag = (PersistentBag) lazyItems;
					assertThat( lazyBag.getOwner(), sameInstance( order ) );
					assertThat( lazyBag.getKey(), is( 1L ) );
					assertThat(
							lazyBag.getRole(),
							is( Order.class.getName() + ".lazyItems" )
					);
					assertFalse( lazyBag.wasInitialized() );
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
					assertThat( lazyItems.size(), is( 0 ) );
					assertThat( statistics.getPrepareStatementCount(), is( 3L ) );
				}
		);
	}

	@Entity(name = "Order")
	@Table(name = "`ORDER`")
	public static class Order {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "order", fetch = FetchType.EAGER)
		private List<Item> eagerItems = new ArrayList<>();

		@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
		private List<Item> lazyItems = new ArrayList<>();

		public Order() {
		}

		public List<Item> getEagerItems() {
			return eagerItems;
		}

		public List<Item> getLazyItems() {
			return lazyItems;
		}

		public void setLazyItems(List<Item> lazyItems) {
			this.lazyItems = lazyItems;
		}
	}

	@Entity(name = "Item")
	public static class Item {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne
		private Order order;
	}
}
