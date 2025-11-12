/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.internal.CollectionCacheInvalidator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Christian Beikov
 */
@JiraKey(value = "HHH-4910")
@DomainModel(
		annotatedClasses = {
				TransactionalConcurrencyCollectionCacheEvictionTest.Person.class,
				TransactionalConcurrencyCollectionCacheEvictionTest.Phone.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.FORMAT_SQL, value = "false" ),
				@Setting( name = Environment.AUTO_EVICT_COLLECTION_CACHE, value = "true" ),
				@Setting( name = Environment.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = Environment.USE_QUERY_CACHE, value = "false" ),
		}
)
public class TransactionalConcurrencyCollectionCacheEvictionTest {
	@BeforeEach
	public void before(SessionFactoryScope scope) {
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = true;

		scope.inTransaction( s -> {
					Person bart = new Person( 1L, "Bart" );
					Person lisa = new Person( 2L, "Lisa" );
					Person maggie = new Person( 3L, "Maggie" );
					s.persist( bart );
					s.persist( lisa );
					s.persist( maggie );

					bart.addPhone( "0-1122334455" );
					bart.addPhone( "0-2233445566" );
					bart.addPhone( "0-3344556677" );
					bart.addPhone( "0-4455667788" );
					bart.addPhone( "0-5566778899" );

					lisa.addPhone( "1-1122334455" );
					lisa.addPhone( "1-2233445566" );
					lisa.addPhone( "1-3344556677" );
					lisa.addPhone( "1-4455667788" );
					lisa.addPhone( "1-5566778899" );

					maggie.addPhone( "2-1122334455" );
					maggie.addPhone( "2-2233445566" );
					maggie.addPhone( "2-3344556677" );
					maggie.addPhone( "2-4455667788" );
					maggie.addPhone( "2-5566778899" );

					bart.getPhones().forEach( s::persist );
					lisa.getPhones().forEach( s::persist );
					maggie.getPhones().forEach( s::persist );
				}
		);
	}

	@AfterEach
	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.getSessionFactory().getCache().evictAll();
		CollectionCacheInvalidator.PROPAGATE_EXCEPTION = false;
	}

	@Test
	public void testCollectionCacheEvictionInsert(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
					Person bart = s.find( Person.class, 1L );
					assertEquals( 5, bart.getPhones().size() );
					s.persist( new Phone( "test", bart ) );
				}
		);
		scope.inTransaction( s -> {
					Person bart = s.find( Person.class, 1L );
					assertEquals( 6, bart.getPhones().size() );
				}
		);
	}

	@Test
	public void testCollectionCacheEvictionRemove(SessionFactoryScope scope) {
		Long phoneId = scope.fromTransaction( s -> {
					Person bart = s.find( Person.class, 1L );
					// Lazy load phones
					assertEquals( 5, bart.getPhones().size() );
					return bart.getPhones().iterator().next().getId();
				}
		);
		scope.inTransaction( s -> {
					s.remove( s.getReference( Phone.class, phoneId ) );
				}
		);
		scope.inTransaction( s -> {
					Person bart = s.find( Person.class, 1L );
					assertEquals( 4, bart.getPhones().size() );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Person {

		@Id
		@Access(value = AccessType.PROPERTY)
		@Column(name = "PERSONID", nullable = false)
		private Long id;

		@Column(name = "NAME")
		private String name;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "person")
		@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
		private final Set<Phone> phones = new HashSet<>();

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public Set<Phone> getPhones() {
			return phones;
		}

		public Phone addPhone(String number) {
			Phone phone = new Phone( number, this );
			getPhones().add( phone );
			return phone;
		}
	}

	@Entity(name = "Phone")
	@Table(name = "PHONE")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Phone {

		@Id
		@Access(value = AccessType.PROPERTY)
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "PHONEID", nullable = false)
		private Long id;

		@Column(name = "PHONE_NUMBER")
		private String number;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "PERSONID")
		private Person person;

		public Phone() {
		}

		public Phone(String number, Person person) {
			this.number = number;
			this.person = person;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}

}
