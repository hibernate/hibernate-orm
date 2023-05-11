package org.hibernate.orm.test.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.stat.CacheRegionStatistics;
import org.hibernate.stat.spi.StatisticsImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

@DomainModel(annotatedClasses = {
		QueryCacheWithMultipleParametersTest.Customer.class
})
@SessionFactory(generateStatistics = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
				@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true")
		}
)
@JiraKey("HHH-16594")
public class QueryCacheWithMultipleParametersTest {

	@Test
	public void testQueryCacheHitsWithSingleCondition(SessionFactoryScope scope) {
		// Create customer John Doe
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );

					Customer c = new Customer(1L, "John", "Doe", "M", 123456L);
					session.persist(c);
				}
		);

		// Query once (miss and populate cache)
		scope.inTransaction(
				session -> {
					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics(session);
					assertThat(defaultQueryCacheRegionStatistics.getHitCount()).isEqualTo(0);
					assertThat(defaultQueryCacheRegionStatistics.getMissCount()).isEqualTo(0);

					final CriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Customer> queryCustomer = cb.createQuery(Customer.class);
					Root<Customer> customer = queryCustomer.from(Customer.class);
					queryCustomer.where(createPredicateWithSingleCondition(cb, customer));

					Query<Customer> query = session.createQuery(queryCustomer);
					query.setCacheable(true);
					List<Customer> customers = query.getResultList();
					assertThat(customers).hasSize(1);
				}
		);

		scope.inTransaction(
				session -> {
					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics(session);
					assertThat(defaultQueryCacheRegionStatistics.getHitCount()).isEqualTo(0);
					assertThat(defaultQueryCacheRegionStatistics.getMissCount()).isEqualTo(1);
				}
		);

		// Query 10 more times (10 hits)
		for (int i = 0; i < 10; i++) {
			scope.inTransaction(
					session -> {
						final CriteriaBuilder cb = session.getCriteriaBuilder();
						CriteriaQuery<Customer> queryCustomer = cb.createQuery(Customer.class);
						Root<Customer> customer = queryCustomer.from(Customer.class);
						queryCustomer.where(createPredicateWithSingleCondition(cb, customer));

						Query<Customer> query = session.createQuery(queryCustomer);
						query.setCacheable(true);
						List<Customer> customers = query.getResultList();
						assertThat(customers).hasSize(1);
					}
			);
		}

		scope.inTransaction(
				session -> {
					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics(session);
					assertThat(defaultQueryCacheRegionStatistics.getHitCount()).isEqualTo(10);
					assertThat(defaultQueryCacheRegionStatistics.getMissCount()).isEqualTo(1);
				}
		);
	}

	@Test
	public void testQueryCacheHitsWithMultipleConditions(SessionFactoryScope scope) {
		// Create customer John Doe
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );
					Customer c = new Customer(1L, "John", "Doe", "M", 123456L);
					session.persist(c);
				}
		);

		// Query once (miss and populate cache)
		scope.inTransaction(
				session -> {
					evictQueryRegion( session );

					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics(session);
					assertThat(defaultQueryCacheRegionStatistics.getHitCount()).isEqualTo(0);
					assertThat(defaultQueryCacheRegionStatistics.getMissCount()).isEqualTo(0);

					final CriteriaBuilder cb = session.getCriteriaBuilder();
					CriteriaQuery<Customer> queryCustomer = cb.createQuery(Customer.class);
					Root<Customer> customer = queryCustomer.from(Customer.class);
					queryCustomer.where(createPredicateWithMultipleConditions(cb, customer));

					// Query once -- miss
					Query<Customer> query = session.createQuery(queryCustomer);
					query.setCacheable(true);
					List<Customer> customers = query.getResultList();
					assertThat(customers).hasSize(1);
				}
		);

		scope.inTransaction(
				session -> {
					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics(session);
					assertThat(defaultQueryCacheRegionStatistics.getHitCount()).isEqualTo(0);
					assertThat(defaultQueryCacheRegionStatistics.getMissCount()).isEqualTo(1);
				}
		);

		// Query 10 more times (10 hits)
		for (int i = 0; i < 10; i++) {
			scope.inTransaction(
					session -> {
						final CriteriaBuilder cb = session.getCriteriaBuilder();
						CriteriaQuery<Customer> queryCustomer = cb.createQuery(Customer.class);
						Root<Customer> customer = queryCustomer.from(Customer.class);
						queryCustomer.where(createPredicateWithMultipleConditions(cb, customer));

						Query<Customer> query = session.createQuery(queryCustomer);
						query.setCacheable(true);
						List<Customer> customers = query.getResultList();
						assertThat(customers).hasSize(1);
					}
			);
		}

		scope.inTransaction(
				session -> {
					CacheRegionStatistics defaultQueryCacheRegionStatistics = getQueryCacheRegionStatistics(session);
					assertThat(defaultQueryCacheRegionStatistics.getHitCount()).isEqualTo(10);
					assertThat(defaultQueryCacheRegionStatistics.getMissCount()).isEqualTo(1);
				}
		);
	}

	private Predicate createPredicateWithSingleCondition(CriteriaBuilder cb, Root<Customer> customer) {
		return cb.and(cb.equal(customer.get("firstName"), "John"));
	}

	private Predicate createPredicateWithMultipleConditions(CriteriaBuilder cb, Root<Customer> customer) {
		return cb.and(cb.equal(customer.get("firstName"), "John"),
				cb.equal(customer.get("lastName"), "Doe"),
				cb.equal(customer.get("gender"), "M"),
				cb.equal(customer.get("ssn"), 123456L));
	}

	private static void evictQueryRegion(SessionImplementor session) {
		session.getSessionFactory()
				.getCache()
				.evictQueryRegion( RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME );
		session.getSessionFactory().getStatistics().clear();
	}

	private static CacheRegionStatistics getQueryCacheRegionStatistics(SessionImplementor session) {
		StatisticsImplementor statistics = session.getSessionFactory().getStatistics();
		return statistics.getQueryRegionStatistics( RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME );
	}

	@Entity(name = "Customer")
	public static class Customer {

		@Id
		private Long id;
		private String firstName;
		private String lastName;
		private String gender;
		private Long ssn;

		public Customer() {
		}

		public Customer(Long id, String firstName, String lastName, String gender, Long ssn) {
			this.id = id;
			this.firstName = firstName;
			this.lastName = lastName;
			this.gender = gender;
			this.ssn = ssn;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getGender() {
			return gender;
		}

		public void setGender(String gender) {
			this.gender = gender;
		}

		public Long getSsn() {
			return ssn;
		}

		public void setSsn(Long ssn) {
			this.ssn = ssn;
		}

	}

}
