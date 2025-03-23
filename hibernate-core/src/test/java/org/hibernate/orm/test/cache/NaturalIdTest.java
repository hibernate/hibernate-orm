/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdCache;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@DomainModel(annotatedClasses = { NaturalIdTest.TestEntity.class })
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"),
		@Setting(name = AvailableSettings.USE_QUERY_CACHE, value = "true"),
})
@JiraKey("HHH_18511")
public class NaturalIdTest {

	@Test
	void testGetNaturalIdentifierSnapshot(SessionFactoryScope scope) {
		final var naturalId1 = "id1";
		final var naturalId2 = "id2";

		scope.inTransaction( session -> {
			var testEntity = new TestEntity( 1l, naturalId1, naturalId2, "abc" );
			session.persist( testEntity );
			session.flush();

			var sessionFactory = session.getFactory();
			var entityPersister = sessionFactory.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( TestEntity.class );

			Object[] naturalId = (Object[]) entityPersister.getNaturalIdentifierSnapshot( 1l, session );
			assertThat( naturalId[0] ).isEqualTo( naturalId1 );
			assertThat( naturalId[1] ).isEqualTo( naturalId2 );
		} );
	}

	@Entity(name = "CompositeNaturalIdModel")
	@Table(name = "CompositeNaturalIdModel")
	@NaturalIdCache
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class TestEntity {

		@Id
		private Long id;

		@NaturalId
		private String naturalId1;

		@NaturalId
		private String naturalId2;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String naturalId1, String naturalId2, String name) {
			this.id = id;
			this.naturalId1 = naturalId1;
			this.naturalId2 = naturalId2;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getNaturalId1() {
			return naturalId1;
		}


		public String getNaturalId2() {
			return naturalId2;
		}

		public String getName() {
			return name;
		}
	}
}
