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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.cache.ehcache.EhCacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;


/**
 * @author Vlad Mihalcea
 */
public class FirstLevelCacheTest extends BaseEntityManagerFunctionalTestCase {

	private static final Logger log = Logger.getLogger( FirstLevelCacheTest.class );

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class
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
        Person aPerson = doInJPA( this::entityManagerFactory, entityManager -> {
            entityManager.persist( new Person() );
            entityManager.persist( new Person() );
			Person person = new Person();
            entityManager.persist( person );
			return person;
        });
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Object> dtos = new ArrayList<>(  );
			//tag::caching-management-jpa-detach-example[]
			for(Person person : entityManager.createQuery("select p from Person p", Person.class)
					.getResultList()) {
				dtos.add(toDTO(person));
				entityManager.detach( person );
			}
			//end::caching-management-jpa-detach-example[]
			//tag::caching-management-clear-example[]
			entityManager.clear();

			//end::caching-management-clear-example[]

			Person person = aPerson;

			//tag::caching-management-contains-example[]
			entityManager.contains( person );

			//end::caching-management-contains-example[]
		});
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Object> dtos = new ArrayList<>(  );
			//tag::caching-management-native-evict-example[]
			Session session = entityManager.unwrap( Session.class );
			for(Person person : (List<Person>) session.createQuery("select p from Person p").list()) {
				dtos.add(toDTO(person));
				session.evict( person );
			}
			//end::caching-management-native-evict-example[]
			//tag::caching-management-clear-example[]
			session.clear();
			//end::caching-management-clear-example[]

			Person person = aPerson;

			//tag::caching-management-contains-example[]
			session.contains( person );
			//end::caching-management-contains-example[]
		});
    }

	private Object toDTO(Person person) {
		return person;
	}


	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		public Person() {
		}

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
	}
}
