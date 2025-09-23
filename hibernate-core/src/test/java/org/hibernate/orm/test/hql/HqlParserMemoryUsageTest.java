/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.query.hql.HqlTranslator;
import org.hibernate.testing.memory.MemoryUsageUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				HqlParserMemoryUsageTest.Address.class,
				HqlParserMemoryUsageTest.AppUser.class,
				HqlParserMemoryUsageTest.Category.class,
				HqlParserMemoryUsageTest.Discount.class,
				HqlParserMemoryUsageTest.Order.class,
				HqlParserMemoryUsageTest.OrderItem.class,
				HqlParserMemoryUsageTest.Product.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.QUERY_PLAN_CACHE_ENABLED, value = "false"))
@Jira("https://hibernate.atlassian.net/browse/HHH-19240")
public class HqlParserMemoryUsageTest {

	private static final String HQL = "SELECT DISTINCT u.id\n" +
									  "FROM AppUser u\n" +
									  "LEFT JOIN u.addresses a\n" +
									  "LEFT JOIN u.orders o\n" +
									  "LEFT JOIN o.orderItems oi\n" +
									  "LEFT JOIN oi.product p\n" +
									  "LEFT JOIN p.discounts d\n" +
									  "WHERE u.id = :userId\n" +
									  "AND (\n" +
									  "	CASE\n" +
									  "		WHEN u.name = 'SPECIAL_USER' THEN TRUE\n" +
									  "		ELSE (\n" +
									  "			CASE\n" +
									  "				WHEN a.city = 'New York' THEN TRUE\n" +
									  "				ELSE (\n" +
									  "					p.category.name = 'Electronics'\n" +
									  "					OR d.code LIKE '%DISC%'\n" +
									  "					OR u.id IN (\n" +
									  "						SELECT u2.id\n" +
									  "						FROM AppUser u2\n" +
									  "						JOIN u2.orders o2\n" +
									  "						JOIN o2.orderItems oi2\n" +
									  "						JOIN oi2.product p2\n" +
									  "						WHERE p2.price > (\n" +
									  "							SELECT AVG(p3.price) FROM Product p3\n" +
									  "						)\n" +
									  "					)\n" +
									  "				)\n" +
									  "			END\n" +
									  "		)\n" +
									  "	END\n" +
									  ")\n";


	@Test
	public void testParserMemoryUsage(SessionFactoryScope scope) {
		final HqlTranslator hqlTranslator = scope.getSessionFactory().getQueryEngine().getHqlTranslator();

		// Ensure classes and basic stuff is initialized in case this is the first test run
		hqlTranslator.translate( "from AppUser", AppUser.class );

		// During testing, before the fix for HHH-19240, the allocation was around 500+ MB,
		// and after the fix it dropped to 170 - 250 MB
		final long memoryUsage = MemoryUsageUtil.estimateMemoryUsage( () -> hqlTranslator.translate( HQL, Long.class ) );
		System.out.println( "Memory Consumption: " + (memoryUsage / 1024) + " KB" );
		assertTrue( memoryUsage < 256_000_000, "Parsing of queries consumes too much memory (" + ( memoryUsage / 1024 ) + " KB), when at most 256 MB are expected" );
	}

	@Entity(name = "Address")
	@Table(name = "addresses")
	public static class Address {
		@Id
		private Long id;
		private String city;
		@ManyToOne(fetch = FetchType.LAZY)
		private AppUser user;
	}
	@Entity(name = "AppUser")
	@Table(name = "app_users")
	public static class AppUser {
		@Id
		private Long id;
		private String name;
		@OneToMany(mappedBy = "user")
		private Set<Address> addresses;
		@OneToMany(mappedBy = "user")
		private Set<Order> orders;
	}

	@Entity(name = "Category")
	@Table(name = "categories")
	public static class Category {
		@Id
		private Long id;
		private String name;
	}

	@Entity(name = "Discount")
	@Table(name = "discounts")
	public static class Discount {
		@Id
		private Long id;
		private String code;
		@ManyToOne(fetch = FetchType.LAZY)
		private Product product;
	}

	@Entity(name = "Order")
	@Table(name = "orders")
	public static class Order {
		@Id
		private Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		private AppUser user;
		@OneToMany(mappedBy = "order")
		private Set<OrderItem> orderItems;
	}
	@Entity(name = "OrderItem")
	@Table(name = "order_items")
	public static class OrderItem {
		@Id
		private Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		private Order order;
		@ManyToOne(fetch = FetchType.LAZY)
		private Product product;
	}

	@Entity(name = "Product")
	@Table(name = "products")
	public static class Product {
		@Id
		private Long id;
		private String name;
		private Double price;
		@ManyToOne(fetch = FetchType.LAZY)
		private Category category;
		@OneToMany(mappedBy = "product")
		private Set<Discount> discounts;
	}
}
