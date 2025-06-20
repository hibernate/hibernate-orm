/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchmode.toone;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.sql.ast.SqlAstJoinType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				LazyToOneWithSelectFetchModeTests.RootEntity.class,
				LazyToOneWithSelectFetchModeTests.SimpleEntity.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
		}
)
public class LazyToOneWithSelectFetchModeTests {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleEntity manyToOneSimpleEntity = new SimpleEntity( 1, "manyToOne" );
			SimpleEntity oneToOneSimpleEntity = new SimpleEntity( 2, "oneToOne" );
			session.persist( manyToOneSimpleEntity );
			session.persist( oneToOneSimpleEntity );

			RootEntity rootEntity = new RootEntity( 1, "root" );
			rootEntity.manyToOneSimpleEntity = manyToOneSimpleEntity;
			rootEntity.oneToOneSimpleEntity = oneToOneSimpleEntity;
			session.persist( rootEntity );
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.find( RootEntity.class, 1 );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( false ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( false ) );

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls, hasSize( 1 ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
				}
		);
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"from RootEntity r where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 1 ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );

					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( false ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( false ) );
				}
		);
	}

	@Test
	public void testHqlJoinManyToOne(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"select r from RootEntity r join r.manyToOneSimpleEntity where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 1 ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );

					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( false ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( false ) );
				}
		);
	}

	@Test
	public void testHqlJoinOneToOne(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"select r from RootEntity r join r.oneToOneSimpleEntity where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 1 ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );

					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( false ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( false ) );

				}
		);
	}

	@Test
	public void testHqlJoinFetchManyToOne(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"from RootEntity r join fetch r.manyToOneSimpleEntity where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( false ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat( firstStatement, containsString( " join simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );
				}
		);
	}

	@Test
	public void testHqlJoinManyToOneAndOneToOne(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"from RootEntity r join r.manyToOneSimpleEntity join r.oneToOneSimpleEntity where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( false ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( false ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat( firstStatement, containsString( " join simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 2 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 2 );
				}
		);
	}

	@Test
	public void testHqlJoinFetchOneToOne(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"from RootEntity r join fetch r.oneToOneSimpleEntity where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( false ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat( firstStatement, containsString( " join simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );
				}
		);
	}

	@Test
	public void testHqlJoinFetchManyToOneAndOneToOne(SessionFactoryScope scope) {
		SQLStatementInspector sqlStatementInterceptor = scope.getCollectingStatementInspector();
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"from RootEntity r join fetch r.manyToOneSimpleEntity join fetch  r.oneToOneSimpleEntity where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 2 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "RootEntity")
	@Table(name = "root_entity")
	public static class RootEntity {
		@Id
		private Integer id;
		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		private SimpleEntity manyToOneSimpleEntity;

		@OneToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		private SimpleEntity oneToOneSimpleEntity;

		public RootEntity() {
		}

		public RootEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

	}

	@Entity(name = "SimpleEntity")
	@Table(name = "simple_entity")
	public static class SimpleEntity {

		@Id
		private Integer id;

		private String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
