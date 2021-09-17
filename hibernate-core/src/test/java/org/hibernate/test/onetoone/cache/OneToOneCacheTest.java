package org.hibernate.test.onetoone.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cache.spi.CacheImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import javax.persistence.*;

public class OneToOneCacheTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] {
            "onetoone/cache/Details.hbm.xml",
            "onetoone/cache/Person.hbm.xml",
        };
    }
    
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Product.class,
				ProductConfig.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, "true");
		configuration.setProperty(AvailableSettings.JPA_SHARED_CACHE_MODE, "ENABLE_SELECTIVE");
		configuration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
    }

	private <TPerson extends Person, TDetails extends Details> void OneToOneTest(Class<TPerson> personClass,
			Class<TDetails> detailsClass) throws Exception {

		// Initialize the database with data.
		List<Serializable> ids = createPersonsAndDetails(personClass, detailsClass);

		// Clear the second level cache and the statistics.
		SessionFactoryImplementor sfi = sessionFactory();
		CacheImplementor cache = sfi.getCache();
		StatisticsImplementor statistics = sfi.getStatistics();

		cache.evictEntityData(personClass);
		cache.evictEntityData(detailsClass);
		cache.evictQueryRegions();

		statistics.clear();

		// Fill the empty caches with data.
		this.getPersons(personClass, ids);

		// Verify that no data was retrieved from the cache.
		assertEquals("Second level cache hit count", 0, statistics.getSecondLevelCacheHitCount());

		statistics.clear();

		this.getPersons(personClass, ids);

		// Verify that all data was retrieved from the cache.
		assertEquals("Second level cache miss count", 0, statistics.getSecondLevelCacheMissCount());
	}

	private <TPerson extends Person, TDetails extends Details> List<Serializable> createPersonsAndDetails(Class<TPerson> personClass,
			Class<TDetails> detailsClass) throws Exception {
		Session s = openSession();
		Transaction tx = s.beginTransaction();

		Constructor<TPerson> ctorPerson = personClass.getConstructor();
		Constructor<TDetails> ctorDetails = detailsClass.getConstructor();
		List<Serializable> ids = new ArrayList<Serializable>();

		for (int i = 0; i < 6; i++) {
			Person person = ctorPerson.newInstance();

			if (i % 2 == 0) {
				Details details = ctorDetails.newInstance();

				details.setData(String.format("%s%d", detailsClass.getName(), i));
				person.setDetails(details);
			}

			person.setName(String.format("%s%d", personClass.getName(), i));

			ids.add(s.save(person));
		}

		tx.commit();
		s.close();

		return ids;
	}

	private <TPerson extends Person> List<TPerson> getPersons(Class<TPerson> personClass, List<Serializable> ids) {		
		Session s = openSession();
		Transaction tx  = s.beginTransaction();
		List<TPerson> people = new ArrayList<TPerson>();

		for (Serializable id : ids) {
			people.add(s.get(personClass, id));	
		}
		
		tx.commit();
		s.close();

		return people;
	}

	@Test
	public void OneToOneCacheByForeignKey() throws Exception {
		OneToOneTest(PersonByFK.class, DetailsByFK.class);
	}

	@Test
	public void OneToOneCacheByRef() throws Exception {
		OneToOneTest(PersonByRef.class, DetailsByRef.class);
	}

	@Test
	public void testFieldShouldNotBeNull2() {
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
