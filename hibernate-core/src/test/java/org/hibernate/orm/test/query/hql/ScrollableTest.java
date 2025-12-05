/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				ScrollableTest.MyEntity.class
		}
)
@SessionFactory
public class ScrollableTest {

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new MyEntity( 1L, "entity_1" ) );
			session.persist( new MyEntity( 2L, "entity_2" ) );
		} );
	}

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@JiraKey(value = "HHH-10860")
	public void testScrollableResults(SessionFactoryScope scope) {
		final List<Long> params = new ArrayList<>();
		params.add( 1L );
		params.add( 2L );
		scope.inSession(
				session -> {
					final Query<MyEntity> query = session.createQuery( "from MyEntity e where e.id in (:ids)",
									MyEntity.class )
							.setParameter( "ids", params )
							.setFetchSize( 10 );
					try (ScrollableResults<MyEntity> scroll = query.scroll( ScrollMode.FORWARD_ONLY )) {
						int i = 0;
						while ( scroll.next() ) {
							if ( i == 0 ) {
								assertThat( scroll.get().getDescription() ).isEqualTo( "entity_1" );
							}
							else {
								assertThat( scroll.get().getDescription() ).isEqualTo( "entity_2" );
							}
							i++;
						}
					}
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10860")
	public void testScrollableResults2(SessionFactoryScope scope) {
		final List<Long> params = new ArrayList<>();
		params.add( 1L );
		params.add( 2L );

		scope.inSession( session -> {
			final Query<MyEntity> query = session.createQuery( "from MyEntity e where e.id in (:ids)", MyEntity.class )
					.setParameter( "ids", params )
					.setFetchSize( 10 );
			try (ScrollableResults<MyEntity> scroll = query.scroll()) {
				int i = 0;
				while ( scroll.next() ) {
					if ( i == 0 ) {
						assertThat( scroll.get().getDescription() ).isEqualTo( "entity_1" );
					}
					else {
						assertThat( scroll.get().getDescription() ).isEqualTo( "entity_2" );
					}
					i++;
				}
			}
		} );
	}

	@Entity(name = "MyEntity")
	@Table(name = "MY_ENTITY")
	public static class MyEntity {
		@Id
		private Long id;

		private String description;

		public MyEntity() {
		}

		public MyEntity(Long id, String description) {
			this.id = id;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}
	}

}
