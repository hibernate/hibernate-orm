/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.NoResultException;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				FullLayoutQueryCacheTest.FirstEntity.class,
				FullLayoutQueryCacheTest.SecondEntity.class,
		}
)
@ServiceRegistry(
		settings = {
				@Setting(
						name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true"
				),
				@Setting(
						name = AvailableSettings.USE_QUERY_CACHE, value = "true"
				),
				@Setting(
						name = AvailableSettings.QUERY_CACHE_LAYOUT, value = "FULL"
				)
		}
)
@SessionFactory
@JiraKey("HHH-18323")
public class FullLayoutQueryCacheTest {

	private static final String FIRST_ENTITY_NAME = "FirstEntity";

	@BeforeEach
	public void setup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SecondEntity secondEntity = new SecondEntity( "second" );
					session.persist( new FirstEntity( FIRST_ENTITY_NAME, secondEntity ) );
					session.persist( secondEntity );
				}
		);
	}

	@Test
	public void testQueryCache(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					session.createQuery(
									"select f from FirstEntity f where f.name = :name", FirstEntity.class )
							.setParameter( "name", FIRST_ENTITY_NAME )
							.setCacheable( true )
							.getSingleResult();
				}
		);

		deleteEntitiesSilently( scope );

		scope.inSession(
				session -> {
					FirstEntity firstEntity = session.createQuery(
									"select f from FirstEntity f where f.name = :name", FirstEntity.class )
							.setParameter( "name", FIRST_ENTITY_NAME )
							.setCacheable( true )
							.getSingleResult();
					assertThat( firstEntity ).isNotNull();
				}
		);

		clearCache( scope );

		scope.inSession( session -> {
			assertThatThrownBy( () ->
					session.createQuery(
									"select f from FirstEntity f where f.name = :name", FirstEntity.class )
							.setParameter( "name", FIRST_ENTITY_NAME )
							.setCacheable( true )
							.getSingleResult()
			).isInstanceOf( NoResultException.class );

		} );
	}

	private static void clearCache(SessionFactoryScope scope) {
		scope.getSessionFactory().getCache().evictAll();
		scope.getSessionFactory().getCache().evictQueryRegions();
	}

	private void deleteEntitiesSilently(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						session.doWork(
								connection -> {
									Statement stmt = null;
									try {
										stmt = connection.createStatement();
										stmt.executeUpdate( "DELETE FROM SecondEntity" );
										stmt.executeUpdate( "DELETE FROM FirstEntity" );
									}
									finally {
										if ( stmt != null ) {
											stmt.close();
										}
									}
								}
						)
		);
	}

	@MappedSuperclass
	public static abstract class BaseEntity {

		@Id
		@GeneratedValue
		protected Long id;

		@Version
		protected int version;

		protected String name;

		public BaseEntity() {
		}

		public BaseEntity(String name) {
			this.name = name;
		}
	}

	@Entity(name = "FirstEntity")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class FirstEntity extends BaseEntity {

		@OneToOne(mappedBy = "firstEntity")
		private SecondEntity secondEntity;

		public FirstEntity() {
		}

		public FirstEntity(String name, SecondEntity secondEntity) {
			super( name );
			this.secondEntity = secondEntity;
			secondEntity.firstEntity = this;
		}
	}

	@Entity(name = "SecondEntity")
	@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
	public static class SecondEntity extends BaseEntity {

		@OneToOne
		private FirstEntity firstEntity;

		public SecondEntity() {
		}

		public SecondEntity(String baseName) {
			super( baseName );
		}
	}
}
