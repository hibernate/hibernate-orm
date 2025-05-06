/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.caching;

import org.hibernate.annotations.NaturalId;
import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = NaturalIdCacheKeyCreationTests.TheEntity.class )
@SessionFactory
public class NaturalIdCacheKeyCreationTests {

	@Test
	public void testSimpleKeyCreation(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityPersister entityDescriptor = (EntityPersister) session
					.getFactory()
					.getRuntimeMetamodels()
					.getEntityMappingType( TheEntity.class );

			SimpleCacheKeysFactory.INSTANCE.createEntityKey( 1, entityDescriptor, session.getSessionFactory(), null );
			SimpleCacheKeysFactory.INSTANCE.createNaturalIdKey( "Steve", entityDescriptor, session );

		} );
	}

	@Test
	public void testDefaultKeyCreation(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityPersister entityDescriptor = (EntityPersister) session
					.getFactory()
					.getRuntimeMetamodels()
					.getEntityMappingType( TheEntity.class );

			DefaultCacheKeysFactory.INSTANCE.createEntityKey( 1, entityDescriptor, session.getSessionFactory(), null );
			DefaultCacheKeysFactory.INSTANCE.createNaturalIdKey( "Steve", entityDescriptor, session );

		} );
	}

	@Entity( name = "TheEntity" )
	@Table( name = "`entities`" )
	public static class TheEntity {
		@Id
		private Integer id;
		@Basic
		@NaturalId
		private String name;

		private TheEntity() {
			// for use by Hibernate
		}

		public TheEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
