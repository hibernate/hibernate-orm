/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.querycache;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.CacheMode;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalceca
 */
@TestForIssue( jiraKey = "HHH-12107" )
public class StructuredQueryCacheTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
			OneToManyWithEmbeddedId.class,
			OneToManyWithEmbeddedIdChild.class,
			OneToManyWithEmbeddedIdKey.class
		};
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( AvailableSettings.USE_QUERY_CACHE, "true" );
		settings.put( AvailableSettings.CACHE_REGION_PREFIX, "foo" );
		settings.put( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
		settings.put( AvailableSettings.GENERATE_STATISTICS, "true" );
		settings.put( AvailableSettings.USE_STRUCTURED_CACHE, "true" );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "transactional";
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12107" )
	public void testEmbeddedIdInOneToMany() {

		OneToManyWithEmbeddedIdKey key = new OneToManyWithEmbeddedIdKey( 1234 );
		final OneToManyWithEmbeddedId o = new OneToManyWithEmbeddedId( key );
		o.setItems( new HashSet<>() );
		o.getItems().add( new OneToManyWithEmbeddedIdChild( 1 ) );

		doInHibernate( this::sessionFactory, session -> {
			session.persist( o );
		});

		doInHibernate( this::sessionFactory, session -> {
			OneToManyWithEmbeddedId _entity = session.find( OneToManyWithEmbeddedId.class, key );
			assertTrue( session.getSessionFactory().getCache().containsEntity( OneToManyWithEmbeddedId.class, key ) );
			assertNotNull( _entity );
		});

		doInHibernate( this::sessionFactory, session -> {
			OneToManyWithEmbeddedId _entity = session.find( OneToManyWithEmbeddedId.class, key );
			assertTrue( session.getSessionFactory().getCache().containsEntity( OneToManyWithEmbeddedId.class, key ) );
			assertNotNull( _entity );
		});
	}

	@Entity(name = "OneToManyWithEmbeddedId")
	public static class OneToManyWithEmbeddedId {

		private OneToManyWithEmbeddedIdKey id;

		private Set<OneToManyWithEmbeddedIdChild> items = new HashSet<>(  );

		public OneToManyWithEmbeddedId() {
		}

		public OneToManyWithEmbeddedId(OneToManyWithEmbeddedIdKey id) {
			this.id = id;
		}

		@EmbeddedId
		public OneToManyWithEmbeddedIdKey getId() {
			return id;
		}

		public void setId(OneToManyWithEmbeddedIdKey id) {
			this.id = id;
		}

		@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = OneToManyWithEmbeddedIdChild.class, orphanRemoval = true)
		@JoinColumn(name = "parent_id")
		public Set<OneToManyWithEmbeddedIdChild> getItems() {
			return items;
		}

		public void setItems(Set<OneToManyWithEmbeddedIdChild> items) {
			this.items = items;
		}
	}

	@Entity(name = "OneToManyWithEmbeddedIdChild")
	public static class OneToManyWithEmbeddedIdChild {
		private Integer id;

		public OneToManyWithEmbeddedIdChild() {
		}

		public OneToManyWithEmbeddedIdChild(Integer id) {
			this.id = id;
		}

		@Id
		@Column(name = "id")
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class OneToManyWithEmbeddedIdKey implements Serializable {
		private Integer id;

		public OneToManyWithEmbeddedIdKey() {
		}

		public OneToManyWithEmbeddedIdKey(Integer id) {
			this.id = id;
		}

		@Column(name = "id")
		public Integer getId() {
			return this.id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}