/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetoone;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hibernate.orm.test.mapping.onetoone.ToOneSelfReferenceTest.EntityTest;

/**
 * @author Andrea Boriero
 */
@DomainModel( annotatedClasses = { EntityTest.class } )
@SessionFactory( useCollectingStatementInspector = true )
public class ToOneSelfReferenceTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityTest entity = new EntityTest( 1, "e1" );
					final EntityTest entity2 = new EntityTest( 2, "e2" );
					final EntityTest entity3 = new EntityTest( 3, "e3" );

					entity2.setEntity( entity3 );
					entity.setEntity( entity2 );
					session.persist( entity3 );
					session.persist( entity2 );
					session.persist( entity );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final EntityTest entity = session.find( EntityTest.class, 1 );
					assertThat( entity.getName(), is( "e1" ) );

					final EntityTest entity2 = entity.getEntity();
					assertThat( entity2, notNullValue() );
					assertThat( entity2.getName(), is( "e2" ) );

					final EntityTest entity3 = entity2.getEntity();
					assertThat( entity3, notNullValue() );
					assertThat( entity3.getName(), is( "e3" ) );

					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16488" )
	public void testJoinIdQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Tuple result = session.createQuery(
					"select e1.id, e1.name, e2.id, e2.name from EntityTest e1 join e1.entity e2 where e1.id = 2",
					Tuple.class
			).getSingleResult();
			assertThat( result.get( 0 ), is( 2 ) );
			assertThat( result.get( 1 ), is( "e2" ) );
			assertThat( result.get( 2 ), is( 3 ) );
			assertThat( result.get( 3 ), is( "e3" ) );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16488" )
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityTest result = session.createQuery(
					"select e from EntityTest e where e.id = 2",
					EntityTest.class
			).getSingleResult();
			assertThat( result.getId(), is( 2 ) );
			assertThat( result.getName(), is( "e2" ) );
			assertThat( result.getEntity().getId(), is( 3 ) );
			assertThat( result.getEntity().getName(), is( "e3" ) );
			assertThat( result.getEntity().getEntity(), is( nullValue() ) );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16488" )
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityTest result = session.find( EntityTest.class, 2 );
			assertThat( result.getId(), is( 2 ) );
			assertThat( result.getName(), is( "e2" ) );
			assertThat( result.getEntity().getId(), is( 3 ) );
			assertThat( result.getEntity().getName(), is( "e3" ) );
			assertThat( result.getEntity().getEntity(), is( nullValue() ) );
		} );
	}

	@Entity( name = "EntityTest" )
	public static class EntityTest {
		@Id
		private Integer id;

		private String name;

		@ManyToOne
		private EntityTest entity;

		public EntityTest() {
		}

		public EntityTest(int id, String name) {
			this.id = id;
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

		public EntityTest getEntity() {
			return entity;
		}

		public void setEntity(EntityTest entity) {
			this.entity = entity;
		}
	}
}
