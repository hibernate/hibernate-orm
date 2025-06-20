/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.onetoone;

import java.util.Calendar;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithLazyOneToOne;
import org.hibernate.testing.orm.domain.gambit.SimpleEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EntityWithLazyOneToOne.class,
				SimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EntityWithLazyBidirectionalOneToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {

		scope.inTransaction( session -> {
			EntityWithLazyOneToOne entity = new EntityWithLazyOneToOne( 1, "first", Integer.MAX_VALUE );

			SimpleEntity other = new SimpleEntity(
					2,
					Calendar.getInstance().getTime(),
					null,
					Integer.MAX_VALUE,
					Long.MAX_VALUE,
					null
			);

			entity.setOther( other );
			session.persist( other );
			session.persist( entity );
		} );

		EntityWithLazyOneToOne entity = new EntityWithLazyOneToOne( 3, "second", Integer.MAX_VALUE );


		scope.inTransaction( session -> {
			SimpleEntity other = new SimpleEntity(
					4,
					Calendar.getInstance().getTime(),
					null,
					1,
					Long.MAX_VALUE,
					null
			);

			entity.setOther( other );
			session.persist( other );
			session.persist( entity );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.get( EntityWithLazyOneToOne.class, 1 );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					assertThat( loaded, notNullValue() );
					assertThat( loaded.getName(), equalTo( "first" ) );

					SimpleEntity other = loaded.getOther();
					assertFalse(
							Hibernate.isInitialized( other ),
							"The lazy association should not be initialized"
					);

					assertThat( other, notNullValue() );
					assertThat( other.getId(), equalTo( 2 ) );
					assertFalse(
							Hibernate.isInitialized( other ),
							"getId() should not trigger the lazy association initialization"

					);
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );


					other.getSomeDate();
					assertTrue(
							Hibernate.isInitialized( other ),
							"The lazy association should be initialized"
					);
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );
				}
		);

		scope.inTransaction(
				session -> {
					final SimpleEntity loaded = session.get( SimpleEntity.class, 2 );
					assert loaded != null;
					assertThat( loaded.getSomeInteger(), equalTo( Integer.MAX_VALUE ) );
				}
		);
	}

	@Test
	public void testHqlSelectParentAttribute(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery(
							"select e.name from EntityWithLazyOneToOne e where e.other.id = 2",
							String.class
					).uniqueResult();
					assertThat( value, equalTo( "first" ) );

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testHqlSelectParent(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.createQuery(
							"select e from EntityWithLazyOneToOne e where e.other.id = 2",
							EntityWithLazyOneToOne.class
					).uniqueResult();

					assertThat( loaded.getName(), equalTo( "first" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					SimpleEntity other = loaded.getOther();
					assertFalse(
							Hibernate.isInitialized( other ),
							"The lazy association should not be initialized"
					);

					assertThat( other, notNullValue() );
					assertThat( other.getId(), equalTo( 2 ) );
					assertFalse(
							Hibernate.isInitialized( other ),
							"getId() should not trigger the lazy association initialization"

					);
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );


					other.getSomeDate();
					assertTrue(
							Hibernate.isInitialized( other ),
							"The lazy association should be initialized"
					);
					assertThat( statistics.getPrepareStatementCount(), is( 2L ) );

				}
		);
	}

	@Test
	public void testHqlSelectParentJoinFetch(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.createQuery(
							"select e from EntityWithLazyOneToOne e join fetch e.other where e.other.id = 2",
							EntityWithLazyOneToOne.class
					).uniqueResult();

					assertThat( loaded.getName(), equalTo( "first" ) );
					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );

					SimpleEntity other = loaded.getOther();
					assertTrue(
							Hibernate.isInitialized( other ),
							"The lazy association should not initialized"
					);

					assertThat( other, notNullValue() );
					assertThat( other.getId(), equalTo( 2 ) );
					other.getSomeDate();

					assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Test
	public void testRemove(SessionFactoryScope scope){
		scope.inTransaction(
				session -> {
					final EntityWithLazyOneToOne loaded = session.get( EntityWithLazyOneToOne.class, 1 );
					assert loaded != null;
					assert loaded.getOther() != null;
					session.remove( loaded );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityWithLazyOneToOne notfound = session.find( EntityWithLazyOneToOne.class, 1 );
					assertThat( notfound, CoreMatchers.nullValue() );
				}
		);

	}
}
