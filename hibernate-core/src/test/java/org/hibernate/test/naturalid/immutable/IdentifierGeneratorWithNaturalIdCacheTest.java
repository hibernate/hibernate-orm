/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.immutable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Alex Burgel
 */
@RequiresDialectFeature( value = DialectChecks.SupportsIdentityColumns.class, jiraKey = "HHH-11330")
public class IdentifierGeneratorWithNaturalIdCacheTest
		extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(
				AvailableSettings.CACHE_REGION_FACTORY,
				CachingRegionFactory.class.getName()
		);
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10659")
	public void testNaturalIdCacheEntry() {
		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.setName( "John Doe" );
			session.persist( person );
		} );
		doInHibernate( this::sessionFactory, session -> {
			assertEquals(0, sessionFactory().getStatistics().getSecondLevelCacheHitCount());
			assertEquals(0, sessionFactory().getStatistics().getNaturalIdCacheHitCount());
			Person person = session.bySimpleNaturalId( Person.class )
					.load( "John Doe" );
			assertEquals(0, sessionFactory().getStatistics().getSecondLevelCacheHitCount());
			assertEquals(1, sessionFactory().getStatistics().getNaturalIdCacheHitCount());
		} );
		doInHibernate( this::sessionFactory, session -> {
			Person person = session.bySimpleNaturalId( Person.class )
					.load( "John Doe" );
			assertEquals(1, sessionFactory().getStatistics().getSecondLevelCacheHitCount());
			assertEquals(2, sessionFactory().getStatistics().getNaturalIdCacheHitCount());
		} );
	}

	@Entity(name = "Person")
	@NaturalIdCache
	@Cache( usage = CacheConcurrencyStrategy.READ_ONLY )
	public static class Person {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;

		@NaturalId
		private String name;

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
	}
}
