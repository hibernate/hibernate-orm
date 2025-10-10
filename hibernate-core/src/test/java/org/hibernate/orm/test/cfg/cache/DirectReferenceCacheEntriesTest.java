/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg.cache;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-13665")
@DomainModel(
		annotatedClasses = {
				DirectReferenceCacheEntriesTest.TheEntity.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES, value = "true")
)
public class DirectReferenceCacheEntriesTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TheEntity theEntity = new TheEntity();
			theEntity.setId( 1L );
			session.persist( theEntity );
		} );
	}

	@Test
	public void testSelectANonCachablenEntity(SessionFactoryScope scope) {
		scope.inTransaction( session ->
				session.createQuery( "select t from TheEntity t", TheEntity.class ).getResultList()
		);
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
