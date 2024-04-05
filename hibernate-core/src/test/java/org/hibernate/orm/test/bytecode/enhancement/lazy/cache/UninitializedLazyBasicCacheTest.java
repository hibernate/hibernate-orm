/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.cache;

import jakarta.persistence.Basic;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.CacheRegionStatistics;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Aaron Schmischke
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				UninitializedLazyBasicCacheTest.Person.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class UninitializedLazyBasicCacheTest {
	private Long personId;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		this.personId = scope.fromTransaction( s -> {
					final Person person = new Person();
					person.setLazyAttribute( "does_not_matter" );
					s.persist( person );
					return person.getId();
				}
		);
	}

	@Test
	@JiraKey("HHH-11766")
	public void test(SessionFactoryScope scope) {

		scope.getSessionFactory().getStatistics().clear();
		scope.getSessionFactory().getCache().evictAll();

		scope.inTransaction( s -> {
					final Person person = s.get( Person.class, personId );
					assertFalse( Hibernate.isPropertyInitialized( person, "lazyAttribute" ) );
				}
		);

		CacheRegionStatistics regionStatistics = scope.getSessionFactory().getStatistics().getCacheRegionStatistics( "Person" );
		assertEquals( 0, regionStatistics.getHitCount() );
		assertEquals( 1, regionStatistics.getMissCount() );
		assertEquals( 1, regionStatistics.getPutCount() );

		scope.inTransaction( s -> {
					final Person person = s.get( Person.class, personId );
					assertFalse( Hibernate.isPropertyInitialized( person, "lazyAttribute" ) );
					person.getLazyAttribute();
					assertTrue( Hibernate.isPropertyInitialized( person, "lazyAttribute" ) );
				}
		);
		assertEquals( 1, regionStatistics.getHitCount() );
		assertEquals( 1, regionStatistics.getMissCount() );
		assertEquals( 1, regionStatistics.getPutCount() );
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
