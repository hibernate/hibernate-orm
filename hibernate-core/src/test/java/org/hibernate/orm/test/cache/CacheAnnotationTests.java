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
import org.hibernate.cfg.Environment;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				CacheAnnotationTests.NoCacheConcurrencyStrategyEntity.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = Environment.USE_SECOND_LEVEL_CACHE, value = "true"),
		}
)
public class CacheAnnotationTests {

	private Integer entityId;


	@Test
	@JiraKey(value = "HHH-12587")
	public void testCacheWriteConcurrencyStrategyNone(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			NoCacheConcurrencyStrategyEntity entity = new NoCacheConcurrencyStrategyEntity();
			session.persist( entity );
			session.flush();
			session.clear();
		} );
	}

	@Test
	@JiraKey(value = "HHH-12868")
	public void testCacheReadConcurrencyStrategyNone(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			NoCacheConcurrencyStrategyEntity entity = new NoCacheConcurrencyStrategyEntity();
			entity.setName( "name" );
			session.persist( entity );
			session.flush();

			this.entityId = entity.getId();

			session.clear();
		} );

		scope.inTransaction( session -> {
			NoCacheConcurrencyStrategyEntity entity = session.getReference( NoCacheConcurrencyStrategyEntity.class,
					this.entityId );
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
