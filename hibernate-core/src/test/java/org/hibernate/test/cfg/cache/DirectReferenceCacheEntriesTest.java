/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cfg.cache;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13665")
public class DirectReferenceCacheEntriesTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, "true" );
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			TheEntity theEntity = new TheEntity();
			theEntity.setId( 1L );
			session.persist( theEntity );
		} );
	}

	@Test
	public void testSelectANonCachablenEntity() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "select t from TheEntity t", TheEntity.class ).getResultList();
		} );
	}

	@Entity(name = "TheEntity")
	@Table(name = "THE_ENTITY")
	@Immutable
	public static class TheEntity {
		@Id
		public Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

}
