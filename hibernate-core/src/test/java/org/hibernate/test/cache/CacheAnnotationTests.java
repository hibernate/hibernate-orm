/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12587")
public class CacheAnnotationTests extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NoCacheConcurrencyStrategyEntity.class };
	}

	@Test
	public void testCacheConcurrencyStrategyNone() {
		doInHibernate( this::sessionFactory, session -> {
			NoCacheConcurrencyStrategyEntity entity = new NoCacheConcurrencyStrategyEntity();
			session.save( entity );
			session.flush();
			session.clear();
		} );
	}

	@Entity(name = "NoCacheConcurrencyStrategy")
	@Cache(usage = CacheConcurrencyStrategy.NONE)
	public static class NoCacheConcurrencyStrategyEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}
}
