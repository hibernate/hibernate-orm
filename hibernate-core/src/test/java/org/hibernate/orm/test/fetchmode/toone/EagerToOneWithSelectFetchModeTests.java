/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.fetchmode.toone;

import java.util.List;
import jakarta.persistence.Entity;
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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = {
				EagerToOneWithSelectFetchModeTests.RootEntity.class,
				EagerToOneWithSelectFetchModeTests.SimpleEntity.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop")
		}
)
public class EagerToOneWithSelectFetchModeTests {

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
					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 3 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );
					assertThat( sqls.get( 0 ), not( containsString( " join " ) ) );
					assertThat( sqls.get( 0 ), containsString( " root_entity " ) );
					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 2 ), containsString( " simple_entity " ) );
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
					assertThat( sqls.size(), is( 3 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 0 );
					assertThat( firstStatement, containsString( " root_entity " ) );

					String secondStatement = sqls.get( 1 );
					assertThat( secondStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );

					String thirthStatement = sqls.get( 2 );
					assertThat( thirthStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 2, 0 );
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
					assertThat( sqls.size(), is( 3 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );

					String secondStatement = sqls.get( 1 );
					assertThat( secondStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );

					String thirthStatement = sqls.get( 2 );
					assertThat( thirthStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 2, 0 );
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
					assertThat( sqls.size(), is( 3 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );

					String secondStatement = sqls.get( 1 );
					assertThat( secondStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );

					String thirthStatement = sqls.get( 2 );
					assertThat( thirthStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 2, 0 );
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
					assertThat( sqls.size(), is( 2 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat( firstStatement, containsString( " join simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );

					String secondStatement = sqls.get( 1 );
					assertThat( secondStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );
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
					assertThat( sqls.size(), is( 3 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 2 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 2 );

					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );

					assertThat( sqls.get( 2 ), containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 2, 0 );
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
					assertThat( sqls.size(), is( 2 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat( firstStatement, containsString( " join simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 0, 1 );
					sqlStatementInterceptor.assertNumberOfJoins( 0, SqlAstJoinType.INNER, 1 );

					String secondStatement = sqls.get( 1 );
					assertThat( secondStatement, containsString( " simple_entity " ) );
					sqlStatementInterceptor.assertNumberOfJoins( 1, 0 );
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
		scope.inTransaction( session -> {
			session.createQuery( "delete from RootEntity" ).executeUpdate();
			session.createQuery( "delete from SimpleEntity" ).executeUpdate();
		} );
	}

	@Entity(name = "RootEntity")
	@Table(name = "root_entity")
	public static class RootEntity {
		@Id
		private Integer id;
		private String name;

		@ManyToOne
		@Fetch(FetchMode.SELECT)
		private SimpleEntity manyToOneSimpleEntity;

		@OneToOne
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
