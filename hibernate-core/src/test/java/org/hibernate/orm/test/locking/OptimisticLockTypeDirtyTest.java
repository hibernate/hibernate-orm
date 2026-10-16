package org.hibernate.orm.test.locking;

import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

import static org.hibernate.cfg.JdbcSettings.ISOLATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
// Test DIRTY predicates at READ_COMMITTED where available. Spanner runs only the basic mapping test.
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry(settingProviders = @SettingProvider(settingName = ISOLATION, provider = OptimisticLockTypeDirtyTest.IsolationProvider.class))
@DomainModel(annotatedClasses = OptimisticLockTypeDirtyTest.Person.class)
@SessionFactory
public class OptimisticLockTypeDirtyTest {
	public static class IsolationProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			var dialect = DialectContext.getDialect();
			return dialect instanceof SpannerDialect || dialect instanceof SpannerPostgreSQLDialect
					? "SERIALIZABLE"
					: "READ_COMMITTED";
		}
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	public void prepareData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			var person = new Person();
			person.setId(1L);
			person.setName("John Doe");
			person.setCountry("US");
			person.setCity("New York");
			person.setCreatedOn(new Timestamp(System.currentTimeMillis()));
			entityManager.persist(person);
		});

	}

	@Test
	public void test(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::locking-optimistic-lock-type-dirty-update-example[]
			var person = entityManager.find(Person.class, 1L);
			person.setCity("Washington D.C.");
			//end::locking-optimistic-lock-type-dirty-update-example[]
		});
	}


	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Concurrent changes can instead fail at SERIALIZABLE isolation")
	public void testNonoverlappingChangesSucceed(SessionFactoryScope factoryScope) {
		try (EntityManager entityManager = factoryScope.getSessionFactory().createEntityManager()) {
			entityManager.getTransaction().begin();
			try {
				Person person = entityManager.find( Person.class, 1L );
				factoryScope.inTransaction( writer -> writer.find( Person.class, 1L ).setName( "Concurrent" ) );
				person.setCity( "Washington D.C." );
				entityManager.getTransaction().commit();
			}
			finally {
				if ( entityManager.getTransaction().isActive() ) {
					entityManager.getTransaction().rollback();
				}
			}
		}
		factoryScope.inTransaction( entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			assertEquals( "Concurrent", person.getName() );
			assertEquals( "Washington D.C.", person.getCity() );
		} );
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Concurrent changes can instead fail at SERIALIZABLE isolation")
	public void testOverlappingChangesConflict(SessionFactoryScope factoryScope) {
		try (EntityManager entityManager = factoryScope.getSessionFactory().createEntityManager()) {
			entityManager.getTransaction().begin();
			try {
				Person person = entityManager.find( Person.class, 1L );
				factoryScope.inTransaction( writer -> writer.find( Person.class, 1L ).setCity( "Concurrent" ) );
				person.setCity( "Stale" );
				assertThrows( OptimisticLockException.class, entityManager::flush );
			}
			finally {
				entityManager.getTransaction().rollback();
			}
		}
		factoryScope.inTransaction( entityManager ->
				assertEquals( "Concurrent", entityManager.find( Person.class, 1L ).getCity() ) );
	}

	//tag::locking-optimistic-lock-type-dirty-example[]
	@Entity(name = "Person")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	public static class Person {

		@Id
		private Long id;

		@Column(name = "`name`")
		private String name;

		private String country;

		private String city;

		@Column(name = "created_on")
		private Timestamp createdOn;

		//Getters and setters are omitted for brevity
	//end::locking-optimistic-lock-type-dirty-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCountry() {
			return country;
		}

		public void setCountry(String country) {
			this.country = country;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public Timestamp getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Timestamp createdOn) {
			this.createdOn = createdOn;
		}
	//tag::locking-optimistic-lock-type-dirty-example[]
	}
	//end::locking-optimistic-lock-type-dirty-example[]
}
