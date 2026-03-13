/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.assertj.core.api.Assertions;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@DomainModel(
		annotatedClasses = QueryGroupFetchStructureValidationTest.DummyEntity.class
)
@SessionFactory
@JiraKey("HHH-20209")
public class QueryGroupFetchStructureValidationTest {

	@BeforeAll
	static void beforeAll(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new DummyEntity( 1 ) );
			session.persist( new DummyEntity( 2 ) );
		} );
	}

	@Test
	void shouldHandleParallelFetchingProjectionWithUnionCorrectly(SessionFactoryScope scope) {
		//given
		final var latch = new CountDownLatch( 1 );
		final Callable<List<DummyProjection>> task = () -> scope.fromSession( (session) -> {
			awaitOnLatch( latch );
			return session.createQuery(
							"""
										select new DummyProjection(d.id) from DummyEntity d where d.id = :id
										union
										select new DummyProjection(d.id) from DummyEntity d where d.id = :id2
									""", DummyProjection.class
					)
					.setParameter( "id", 1L )
					.setParameter( "id2", 2L )
					.getResultList();
		} );
		final var executorService = Executors.newFixedThreadPool( 2 );

		//when
		final var future1 = CompletableFuture.supplyAsync( () -> uncheck( task ), executorService );
		final var future2 = CompletableFuture.supplyAsync( () -> uncheck( task ), executorService );
		latch.countDown();

		//then
		final var results = Stream.of( future1, future2 )
				.map( CompletableFuture::join )
				.toList();
		Assertions.assertThat( results ).allSatisfy( result ->
				Assertions.assertThat( result )
						.extracting( projection -> projection.id )
						.containsExactlyInAnyOrder( 1, 2 ) );
	}

	private void awaitOnLatch(CountDownLatch latch) {
		try {
			if ( !latch.await( 2, TimeUnit.SECONDS ) ) {
				Assertions.fail( "Did not get latch within 2 seconds" );
			}
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
	}

	private <T> T uncheck(Callable<T> callable) {
		try {
			return callable.call();
		}
		catch (Exception e) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "DummyEntity")
	static class DummyEntity {
		@SuppressWarnings("unused")
		@Id
		private Integer id;

		public DummyEntity() {
		}

		public DummyEntity(Integer id) {
			this.id = id;
		}
	}


	record DummyProjection(Integer id) {
	}
}
