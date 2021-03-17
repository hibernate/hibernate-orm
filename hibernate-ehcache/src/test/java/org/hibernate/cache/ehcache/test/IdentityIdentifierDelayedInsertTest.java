/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.ehcache.test;

import java.util.Map;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.FlushMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cache.ehcache.internal.SingletonEhcacheRegionFactory;
import org.hibernate.cfg.AvailableSettings;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
public class IdentityIdentifierDelayedInsertTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( AvailableSettings.FLUSH_MODE, FlushMode.COMMIT );
		settings.put( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "false" );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.USE_QUERY_CACHE, "false" );
		settings.put( AvailableSettings.AUTO_EVICT_COLLECTION_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_FACTORY, SingletonEhcacheRegionFactory.class.getName() );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "false" );
	}

	@Entity(name = "NonCachedEntity")
	public static class NonCachedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String data;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			NonCachedEntity that = (NonCachedEntity) o;
			return Objects.equals( id, that.id ) &&
					Objects.equals( data, that.data );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, data );
		}
	}

	@Entity(name = "CachedEntity")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public static class CachedEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
		private String name;
		@ManyToOne
		@JoinColumn(nullable = false)
		private NonCachedEntity nonCachedEntity;

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

		public NonCachedEntity getNonCachedEntity() {
			return nonCachedEntity;
		}

		public void setNonCachedEntity(NonCachedEntity nonCachedEntity) {
			this.nonCachedEntity = nonCachedEntity;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			CachedEntity that = (CachedEntity) o;
			return Objects.equals( id, that.id ) &&
					Objects.equals( name, that.name ) &&
					Objects.equals( nonCachedEntity, that.nonCachedEntity );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name, nonCachedEntity );
		}
	}

	@Entity(name = "SomeEntity")
	@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
	public static class SomeEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Long id;
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

		@Override
		public final boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SomeEntity asset = (SomeEntity) o;
			if (asset.id == null || id == null) {
				return false;
			}
			return Objects.equals(id, asset.id);
		}

		@Override
		public final int hashCode() {
			return Objects.hashCode(id);
		}

		@Override
		public String toString() {
			return "SomeEntity{" + "id=" + id + ", name='" + name + '\'' + '}';
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class<?>[] { SomeEntity.class, NonCachedEntity.class, CachedEntity.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13147")
	public void testPersistingCachedEntityWithIdentityBasedIdentifier() {
		doInHibernate( this::sessionFactory, session -> {
			SomeEntity entity = new SomeEntity();
			session.persist( entity );

			entity.setName( "foo" );
			session.persist( entity );

			session.flush();
			session.clear();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13164")
	public void testPersistingCachedEntityWithIdentityBasedIdentifierReferencingNonCachedEntity() {
		doInHibernate( this::sessionFactory, session -> {
			final NonCachedEntity nonCachedEntity = new NonCachedEntity();
			nonCachedEntity.setData( "NonCachedEntity" );
			session.persist( nonCachedEntity );

			final CachedEntity cachedEntity = new CachedEntity();
			cachedEntity.setName( "CachedEntity" );
			cachedEntity.setNonCachedEntity( nonCachedEntity );
			session.persist( cachedEntity );
		} );
	}
}
