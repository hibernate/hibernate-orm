/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sqm;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Version;

import static org.junit.Assert.assertEquals;

@JiraKey(value = "HHH-19429")
public class ConcurrentQueryCreationTest extends BaseCoreFunctionalTestCase {

	private static final int NUM_THREADS = 32;

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( Environment.POOL_SIZE, Integer.toString( NUM_THREADS ) );
	}

	@Test
	public void versionedUpdate() throws Exception {
		Consumer<SessionImplementor> action = session -> {
			SimpleEntity entity = new SimpleEntity( "jack" );
			session.persist( entity );
			session.createMutationQuery( "UPDATE VERSIONED SimpleEntity e SET e.name = :name WHERE e.id = :id" )
					.setParameter( "id", entity.getId() )
					.setParameter( "name", "new name" )
					.executeUpdate();
		};

		runConcurrently( action );
	}

	@Test
	public void queryWithTreat() throws Exception {
		Consumer<SessionImplementor> action = session -> {
			SpecificEntity specificEntity = new SpecificEntity( "some name" );
			session.persist( specificEntity );
			session.persist( new OtherSpecificEntity( "some name" ) );

			List<Long> results = session.createQuery(
							"""
									SELECT COUNT(*) FROM ParentEntity e WHERE e.id = :id
									AND (
										TREAT(e as SpecificEntity).name = :name
									OR
										TREAT(e as OtherSpecificEntity).name = :name
									)""", Long.class
					)
					.setParameter( "id", specificEntity.getId() )
					.setParameter( "name", "some name" )
					.getResultList();

			assertEquals( 1, results.size() );
			assertEquals( 1L, results.get( 0 ).longValue() );
		};

		runConcurrently( action );
	}

	private void runConcurrently(Consumer<SessionImplementor> action) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool( NUM_THREADS );
		try {
			CompletionService<Void> completionService = new ExecutorCompletionService<>( executor );

			for ( int round = 0; round < 100; round++ ) {
				for ( int i = 0; i < NUM_THREADS; i++ ) {
					completionService.submit( () -> {
						inTransaction( action );
						return null;
					} );
				}

				for ( int i = 0; i < NUM_THREADS; i++ ) {
					completionService.take().get( 1, TimeUnit.MINUTES );
				}

				rebuildSessionFactory();
			}
		}
		finally {
			executor.shutdown();
			executor.awaitTermination( 1, TimeUnit.MINUTES );
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				SimpleEntity.class,
				ParentEntity.class,
				SpecificEntity.class,
				OtherSpecificEntity.class
		};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@Version
		private Integer version;

		private String name;

		SimpleEntity() {
		}

		SimpleEntity(String name) {
			this.name = name;
		}

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

	@Entity(name = "ParentEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class ParentEntity {
		@Id
		@GeneratedValue
		private Integer id;

		public Integer getId() {
			return id;
		}
	}

	@Entity(name = "SpecificEntity")
	public static class SpecificEntity extends ParentEntity {
		private String name;

		public SpecificEntity() {
		}

		public SpecificEntity(String name) {
			this.name = name;
		}
	}

	@Entity(name = "OtherSpecificEntity")
	public static class OtherSpecificEntity extends ParentEntity {
		private String name;

		public OtherSpecificEntity() {
		}

		public OtherSpecificEntity(String name) {
			this.name = name;
		}
	}
}
