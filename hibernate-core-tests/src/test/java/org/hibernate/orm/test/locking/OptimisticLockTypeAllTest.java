package org.hibernate.orm.test.locking;

import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(annotatedClasses = OptimisticLockTypeAllTest.Person.class)
public class OptimisticLockTypeAllTest {
	@AfterEach
	void tearDown(EntityManagerFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@BeforeEach
	public void prepareData(EntityManagerFactoryScope factoryScope) {
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
	public void test(EntityManagerFactoryScope factoryScope) {
		factoryScope.inTransaction( entityManager -> {
			//tag::locking-optimistic-lock-type-all-update-example[]
			var person = entityManager.find(Person.class, 1L);
			person.setCity("Washington D.C.");
			//end::locking-optimistic-lock-type-all-update-example[]
		});
	}


	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "Concurrent changes can instead fail at SERIALIZABLE isolation")
	public void testNonoverlappingChangesConflict(EntityManagerFactoryScope factoryScope) {
		try (EntityManager entityManager = factoryScope.getEntityManagerFactory().createEntityManager()) {
			entityManager.getTransaction().begin();
			try {
				Person person = entityManager.find( Person.class, 1L );
				factoryScope.inTransaction( writer -> writer.find( Person.class, 1L ).setName( "Concurrent" ) );
				person.setCity( "Washington D.C." );
				assertThrows( OptimisticLockException.class, entityManager::flush );
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
			assertEquals( "New York", person.getCity() );
		} );
	}

	//tag::locking-optimistic-lock-type-all-example[]
	@Entity(name = "Person")
	@OptimisticLocking(type = OptimisticLockType.ALL)
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
	//end::locking-optimistic-lock-type-all-example[]

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
	//tag::locking-optimistic-lock-type-all-example[]
	}
	//end::locking-optimistic-lock-type-all-example[]
}
