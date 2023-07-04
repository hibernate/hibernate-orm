package org.hibernate.orm.test.loaders;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.annotations.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
@DomainModel(annotatedClasses = {
		SqlSelectEntityWithDisabledProxyTest.Order.class,
		SqlSelectEntityWithDisabledProxyTest.OrderLine.class,
		SqlSelectEntityWithDisabledProxyTest.Product.class,
		SqlSelectEntityWithDisabledProxyTest.Cheese.class,
})
public class SqlSelectEntityWithDisabledProxyTest {

	static void setupData(SessionFactoryScope scope) {
		Cheese product1 = new Cheese("Cheese 1");
		Cheese product2 = new Cheese("Cheese 2");

		Order order = new Order();
		order.name = "Hibernate";
		order.orderLines.add(new OrderLine(10, order, product1));
		order.orderLines.add(new OrderLine(-10, order, product2));

		order.products.add(product1);
		order.products.add(product2);

		product1.order = order;
		product2.order = order;

		product1.bestCheese = product1;
		product2.bestCheese = product1;
		product2.bestPairedWith = product1;

		product1.replacement = product2;
		product2.replacement = product1;

		scope.inTransaction(s -> {
			s.persist(product1);
			s.persist(product2);

			s.persist(order);

			for (OrderLine line : order.orderLines) {
				s.persist(line);
			}
		});
	}

	@Test
	void manyToOne(SessionFactoryScope scope) {
		setupData(scope);

		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Order o = s.get( Order.class, 1 );

			assertEquals( 2, o.products.size() );
		});
	}

	@Test
	void sqlSelect(SessionFactoryScope scope) {
		setupData(scope);

		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			Order o = s.get( Order.class, 1 );

			assertEquals( 1, o.purchases.size() );
		});
	}

	@Test
	void criteriaQuery(SessionFactoryScope scope) {
		setupData(scope);

		scope.inSession( s -> {
			s.getSessionFactory().getCache().evictAllRegions();

			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<Product> cr = cb.createQuery(Product.class);
			Root<Product> root = cr.from(Product.class);
			CriteriaQuery<Product> query = cr.select(root);

			List<Product> products = s.createQuery(query).getResultList();

			assertEquals( 2, products.size() );
		});
	}

	@Entity
	static class Order {
		@Id @GeneratedValue
		@Column(name = "id")
		Long id;
		String name;

		@OneToMany(targetEntity = OrderLine.class)
		@SQLSelect(sql = "select id, order_id, product_id, quantity from order_lines where order_id = ? and quantity > 0",
				resultSetMapping = @SqlResultSetMapping(name = "",
						entities = {@EntityResult(entityClass = OrderLine.class,
								fields = {@FieldResult(name = "id", column = "id"),
										@FieldResult(name = "order", column = "order_id"),
										@FieldResult(name = "product", column = "product_id"),
										@FieldResult(name = "quantity", column = "quantity")})}))
		List<OrderLine> purchases = new ArrayList<>();

		@OneToMany(mappedBy = "order")
		List<OrderLine> orderLines = new ArrayList<>();

		@OneToMany(mappedBy = "order")
		List<Product> products = new ArrayList<>();
	}

	@Entity
	@Table(name = "order_lines")
	static class OrderLine {
		@Id @GeneratedValue
		@Column(name = "id")
		Long id;
		int quantity;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "order_id")
		Order order;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "product_id")
		Product product;

		OrderLine() {
		}

		public OrderLine(int quantity, Order order, Product product) {
			this.quantity = quantity;
			this.order = order;
			this.product = product;
		}
	}

	@Entity
	@Proxy(lazy = false)
	@BatchSize(size = 512)
	@Cacheable
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorValue(value = "PRODUCT")
	static class Product {
		@Id @GeneratedValue
		Long id;
		String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "order_id")
		Order order;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		Product replacement;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		Cheese bestPairedWith;

		Product() {
		}

		Product(String name) {
			this.name = name;
		}
	}

	@Entity
	@Proxy(lazy = false)
	@BatchSize(size = 512)
	@Cacheable
	@DiscriminatorValue(value = "CHEESE")
	static class Cheese extends Product {
		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		Cheese bestCheese;

		Cheese() {
		}

		Cheese(String name) {
			super(name);
		}
	}

	@Entity
	@BatchSize(size = 512)
	@Cacheable
	@DiscriminatorValue(value = "STRONG_CHEESE")
	static class StrongCheese extends Cheese {
		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		Cheese strongerCheese;
		StrongCheese() {
		}

		StrongCheese(String name) {
			super(name);
		}
	}
}
