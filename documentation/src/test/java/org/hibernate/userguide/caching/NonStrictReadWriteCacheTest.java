/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.caching;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.ehcache.EhCacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;


/**
 * @author Vlad Mihalcea
 */
public class NonStrictReadWriteCacheTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( NonStrictReadWriteCacheTest.class );

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class,
			Phone.class
        };
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected void addConfigOptions(Map options) {
        options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.TRUE.toString() );
        options.put( AvailableSettings.CACHE_REGION_FACTORY, EhCacheRegionFactory.class.getName() );
    }

    @Test
    public void testCache() {
        doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
            entityManager.persist( person );
			Phone home = new Phone( "123-456-7890" );
			Phone office = new Phone( "098-765-4321" );
			person.addPhone( home );
			person.addPhone( office );
        });
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = entityManager.find( Person.class, 1L );
			person.getPhones().size();
		});
		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Log collection from cache" );
			//tag::caching-collection-example[]
			Person person = entityManager.find( Person.class, 1L );
			person.getPhones().size();
			//end::caching-collection-example[]
		});
        doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Load from cache" );
            entityManager.find( Person.class, 1L ).getPhones().size();
        });
    }


    @Entity(name = "Person")
	@Cacheable
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
    public static class Person {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

		//tag::caching-collection-mapping-example[]
		@OneToMany(mappedBy = "person", cascade = CascadeType.ALL)
		@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
		private List<Phone> phones = new ArrayList<>(  );
		//end::caching-collection-mapping-example[]

        @Version
        private int version;

        public Person() {}

        public Person(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

		public List<Phone> getPhones() {
			return phones;
		}

		public void addPhone(Phone phone) {
			phones.add( phone );
			phone.setPerson( this );
		}
	}

	//tag::caching-entity-mapping-example[]
	@Entity(name = "Phone")
	@Cacheable
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public static class Phone {

		@Id
		@GeneratedValue
		private Long id;

		private String mobile;

		@ManyToOne
		private Person person;

		@Version
		private int version;

		public Phone() {}

		public Phone(String mobile) {
			this.mobile = mobile;
		}

		public Long getId() {
			return id;
		}

		public String getMobile() {
			return mobile;
		}

		public Person getPerson() {
			return person;
		}

		public void setPerson(Person person) {
			this.person = person;
		}
	}
	//end::caching-entity-mapping-example[]
}
