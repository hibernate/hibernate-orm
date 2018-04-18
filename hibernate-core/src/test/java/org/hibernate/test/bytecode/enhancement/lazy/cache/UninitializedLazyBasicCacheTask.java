/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.cache;

import javax.persistence.Basic;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.stat.SecondLevelCacheStatistics;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Aaron Schmischke
 * @author Gail Badner
 */
public class UninitializedLazyBasicCacheTask extends AbstractEnhancerTestTask {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "false" );
		cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
		prepare( cfg );
	}

	public void execute() {

		Session s = getFactory().openSession();
		s.getTransaction().begin();
		Person person = new Person();
		person.setLazyAttribute( "does_not_matter" );
		s.persist( person );
		s.getTransaction().commit();
		s.close();

		final Long personId = person.getId();

		getFactory().getStatistics().clear();
		getFactory().getCache().evictAllRegions();

		s = getFactory().openSession();
		s.getTransaction().begin();
		{
			person = s.get( Person.class, personId );
			assertFalse( Hibernate.isPropertyInitialized( person, "lazyAttribute" ) );
		}
		s.getTransaction().commit();
		s.close();

		SecondLevelCacheStatistics regionStatistics = getFactory().getStatistics().getSecondLevelCacheStatistics(
				"hibernate.test.Person"
		);
		assertEquals( 0, regionStatistics.getHitCount() );
		assertEquals( 1, regionStatistics.getMissCount() );
		assertEquals( 1, regionStatistics.getPutCount() );

		s = getFactory().openSession();
		s.getTransaction().begin();
		{
					person = s.get( Person.class, personId );
					assertFalse( Hibernate.isPropertyInitialized( person, "lazyAttribute" ) );
					person.getLazyAttribute();
					assertTrue( Hibernate.isPropertyInitialized( person, "lazyAttribute" ) );
		}
		s.getTransaction().commit();
		s.close();

		assertEquals( 1, regionStatistics.getHitCount() );
		assertEquals( 1, regionStatistics.getMissCount() );
		assertEquals( 1, regionStatistics.getPutCount() );
	}

	protected void cleanup() {
	}

	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "all", region = "Person")
	@Entity(name = "Person")
	public static class Person {

		@Id
		@GeneratedValue
		@Column(name = "id")
		private Long id;

		@Column(name = "lazyAttribute")
		@Basic(fetch = FetchType.LAZY)
		private String lazyAttribute;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getLazyAttribute() {
			return lazyAttribute;
		}

		public void setLazyAttribute(String lazyAttribute) {
			this.lazyAttribute = lazyAttribute;
		}

	}
}
