/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class CacheAnnotationTests extends BaseCoreFunctionalTestCase {

	private Integer entityId;

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, true );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { NoCacheConcurrencyStrategyEntity.class };
	}

	@Test
	@JiraKey(value = "HHH-12587")
	public void testCacheWriteConcurrencyStrategyNone() {
		doInHibernate( this::sessionFactory, session -> {
			NoCacheConcurrencyStrategyEntity entity = new NoCacheConcurrencyStrategyEntity();
			session.persist( entity );
			session.flush();
			session.clear();
		} );
	}

	@Test
	@JiraKey(value = "HHH-12868")
	public void testCacheReadConcurrencyStrategyNone() {
		doInHibernate( this::sessionFactory, session -> {
			NoCacheConcurrencyStrategyEntity entity = new NoCacheConcurrencyStrategyEntity();
			entity.setName( "name" );
			session.persist( entity );
			session.flush();

			this.entityId = entity.getId();

			session.clear();
		} );

		doInHibernate( this::sessionFactory, session -> {
			NoCacheConcurrencyStrategyEntity entity = session.getReference( NoCacheConcurrencyStrategyEntity.class, this.entityId );
			assertEquals( "name", entity.getName() );
		} );
	}

	@Entity(name = "NoCacheConcurrencyStrategy")
	@Cache(usage = CacheConcurrencyStrategy.NONE)
	public static class NoCacheConcurrencyStrategyEntity {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
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
