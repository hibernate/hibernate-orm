/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sqm;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.Session;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				ConcurrentQueriesByIdsTest.SimpleEntity.class
		}
)
@SessionFactory
public class ConcurrentQueriesByIdsTest {

	public static final String QUERY_STRING = "select e from simple e where e.id in (:ids)";

	@Test
	public void run(SessionFactoryScope scope) throws InterruptedException {
		scope.inTransaction( session -> {
			for ( int i = 0; i < 100; i++ ) {
				SimpleEntity entity = new SimpleEntity();
				entity.setId( i );
				entity.setName( "Name: " + i );
				session.persist( entity );
			}
		} );

		ExecutorService executorService = Executors.newFixedThreadPool( 3 );
		CompletableFuture<List<SimpleEntity>>[] results = new CompletableFuture[10];

		for ( int i = 0; i < 10; i++ ) {
			int index = i;
			results[i] = CompletableFuture.supplyAsync( () -> executeQuery( scope, index ), executorService );
		}
		for ( int i = 0; i < 10; i++ ) {
			assertThat( results[i].join() ).hasSize( 10 );
		}

		executorService.shutdown();
	}

	private List<SimpleEntity> executeQuery(SessionFactoryScope scope, int index) {
		return scope.fromSession(
				session -> executeQuery( session, index )
		);
	}

	private List<SimpleEntity> executeQuery(Session session, int index) {
		int base = index * 10;

		return session.createQuery( QUERY_STRING, SimpleEntity.class )
				.setParameter(
						"ids",
						Arrays.asList( base + 0, base + 1, base + 2, base + 3, base + 4, base + 5,
								base + 6, base + 7, base + 8, base + 9
						)
				)
				.list();
	}

	@Entity(name = "simple")
	public static class SimpleEntity {

		@Id
		private Integer id;

		@Basic
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
