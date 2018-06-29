/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.caching;

import java.util.Map;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;


/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12649" )
public class AutoRegisterCacheTest extends BaseEntityManagerFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
            Person.class
        };
    }

	protected boolean registerCachesManually() {
		return false;
	}

    @Override
    @SuppressWarnings( "unchecked" )
    protected void addConfigOptions(Map options) {
        options.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, Boolean.TRUE.toString() );
        options.put( AvailableSettings.CACHE_REGION_FACTORY, "ehcache" );
        options.put( AvailableSettings.USE_QUERY_CACHE, Boolean.TRUE.toString() );
        options.put( AvailableSettings.GENERATE_STATISTICS, Boolean.TRUE.toString() );
    }

    @Test
    public void testCache() {
        doInJPA( this::entityManagerFactory, entityManager -> {
            entityManager.persist( new Person() );
			Person aPerson= new Person();
			aPerson.setName( "John Doe" );
			aPerson.setCode( "unique-code" );
            entityManager.persist( aPerson );
			return aPerson;
        });

		doInJPA( this::entityManagerFactory, entityManager -> {
			log.info( "Jpa load by id" );
			Person person = entityManager.find( Person.class, 1L );
		});
	}

	@Entity(name = "Person")
	@Cacheable
	@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    public static class Person {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String name;

		@NaturalId
		@Column(name = "code", unique = true)
		private String code;

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

		public String getCode() {
			return code;
		}

		public void setCode(String code) {
			this.code = code;
		}
	}
}
