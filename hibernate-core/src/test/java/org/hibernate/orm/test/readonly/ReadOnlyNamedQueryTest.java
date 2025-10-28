/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.util.function.Consumer;

import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.jpa.HibernateHints.HINT_READ_ONLY;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = ReadOnlyNamedQueryTest.TestEntity.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17313" )
public class ReadOnlyNamedQueryTest extends AbstractReadOnlyTest {
	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSetReadOnly(SessionFactoryScope scope) {
		executeTest( scope, session -> {
			final TestEntity entity = session.createNamedQuery(
					"TestEntity.selectAll",
					TestEntity.class
			).getSingleResult();
			assertThat( entity.getData() ).isEqualTo( "original" );

			session.setReadOnly( entity, true );
			assertThat( session.isReadOnly( entity ) ).isTrue();

			entity.setData( "changed" );
			assertThat( entity.getData() ).isEqualTo( "changed" );
		} );
	}

	@Test
	public void testReadOnlyHint(SessionFactoryScope scope) {
		executeTest( scope, session -> {
			session.setDefaultReadOnly( true );
			final TestEntity entity = session.createNamedQuery(
					"TestEntity.selectAll",
					TestEntity.class
			).setHint( HINT_READ_ONLY, true ).getSingleResult();

			assertThat( entity.getData() ).isEqualTo( "original" );
			assertThat( session.isReadOnly( entity ) ).isTrue();

			entity.setData( "changed" );
			assertThat( entity.getData() ).isEqualTo( "changed" );
		} );
	}

	@Test
	public void testDefaultReadOnly(SessionFactoryScope scope) {
		executeTest( scope, session -> {
			session.setDefaultReadOnly( true );
			final TestEntity entity = session.createNamedQuery(
					"TestEntity.selectAll",
					TestEntity.class
			).getSingleResult();

			assertThat( entity.getData() ).isEqualTo( "original" );
			assertThat( session.isReadOnly( entity ) ).isTrue();

			entity.setData( "changed" );
			assertThat( entity.getData() ).isEqualTo( "changed" );
		} );
	}

	private void executeTest(SessionFactoryScope scope, Consumer<SessionImplementor> namedQueryExecutor) {
		clearCounts( scope );

		scope.inTransaction( session -> {
			final TestEntity entity = new TestEntity( 1, "original" );
			session.persist( entity );
		} );

		assertInsertCount( 1, scope );
		assertUpdateCount( 0, scope );
		clearCounts( scope );

		scope.inTransaction( namedQueryExecutor );

		assertUpdateCount( 0, scope );
		assertUpdateCount( 0, scope );

		scope.inTransaction( session -> {
			final TestEntity entity = session.find( TestEntity.class, 1 );
			assertThat( entity.getData() ).isEqualTo( "original" );
		} );
	}

	@Entity( name = "TestEntity" )
	@NamedQuery( name = "TestEntity.selectAll", query = "select t from TestEntity t" )
	public static class TestEntity {
		@Id
		private Integer id;

		private String data;

		public TestEntity() {

		}

		public TestEntity(Integer id, String data) {
			this.id = id;
			this.data = data;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}
