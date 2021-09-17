package org.hibernate.test.onetoone.cache;

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
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-14826")
public class OneToOneCacheEnableSelectingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class,
				ProductConfig.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
		configuration.setProperty(AvailableSettings.JPA_SHARED_CACHE_MODE, "ENABLE_SELECTIVE");
		configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
	}

	@Test
	public void testFieldShouldNotBeNull() {
		final AtomicLong pid = new AtomicLong();

		// create Product
		inTransaction(s -> {
			Product product = new Product();
			s.persist(product);
			pid.set(product.getId());
		});

		// create ProductConfig and associate with a Product
		inTransaction(s -> {
			Product product = s.find(Product.class, pid.get());
			ProductConfig config = new ProductConfig();
			config.setProduct(product);
			s.persist(config);
		});

		assertTrue(sessionFactory().getCache().containsEntity(Product.class, pid.get()));

		sessionFactory().getStatistics().clear();

		// now fetch the Product again
		inTransaction(s -> {
			Product product = s.find(Product.class, pid.get());

			// should have been from cache
			assertNotEquals (0, sessionFactory().getStatistics().getSecondLevelCacheHitCount());

			// this should not fail
			assertNotNull("one-to-one field should not be null", product.getConfig());
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
