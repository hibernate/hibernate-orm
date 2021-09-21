package org.hibernate.orm.test.onetoone.cache;

import java.util.concurrent.atomic.AtomicLong;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestForIssue( jiraKey = "HHH-14826")
@DomainModel(
		annotatedClasses = {
				OneToOneCacheEnableSelectingTest.Product.class,
				OneToOneCacheEnableSelectingTest.ProductConfig.class
		}
)
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting( name =  AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
				@Setting( name =  AvailableSettings.JPA_SHARED_CACHE_MODE, value = "ENABLE_SELECTIVE")
		}
)
public class OneToOneCacheEnableSelectingTest {

	@Test
	public void testFieldShouldNotBeNull(SessionFactoryScope scope) {
		final AtomicLong pid = new AtomicLong();

		// create Product
		scope.inTransaction(s -> {
			Product product = new Product();
			s.persist(product);
			pid.set(product.getId());
		});

		// create ProductConfig and associate with a Product
		scope.inTransaction(s -> {
			Product product = s.find(Product.class, pid.get());
			ProductConfig config = new ProductConfig();
			config.setProduct(product);
			s.persist(config);
		});

		assertTrue(scope.getSessionFactory().getCache().containsEntity(Product.class, pid.get()));

		scope.getSessionFactory().getStatistics().clear();

		// now fetch the Product again
		scope.inTransaction(s -> {
			Product product = s.find(Product.class, pid.get());

			// should have been from cache
			assertNotEquals (0, scope.getSessionFactory().getStatistics().getSecondLevelCacheHitCount());

			// this should not fail
			assertNotNull( product.getConfig(), "one-to-one field should not be null");
		});
	}

	@Entity(name = "Product")
	@Cacheable
	public static class Product {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		@OneToOne(mappedBy = "product", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		private ProductConfig config;

		public Product() {}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public ProductConfig getConfig() {
			return config;
		}

		public void setConfig(ProductConfig config) {
			this.config = config;
		}
	}

	@Entity(name = "ProductConfig")
	@Cacheable
	public static class ProductConfig {

		@Id
		@GeneratedValue
		private Long id;

		@Version
		private Integer version;

		@OneToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
		private Product product;

		public ProductConfig() {}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}

		public Product getProduct() {
			return product;
		}

		public void setProduct(Product product) {
			this.product = product;
		}
	}

}
