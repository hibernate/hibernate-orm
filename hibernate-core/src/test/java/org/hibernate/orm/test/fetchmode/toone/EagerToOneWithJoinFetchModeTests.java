/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryProducer;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@DomainModel(
		annotatedClasses = {
				EagerToOneWithJoinFetchModeTests.RootEntity.class,
				EagerToOneWithJoinFetchModeTests.SimpleEntity.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.HBM2DDL_DATABASE_ACTION, value = "create-drop")
		}
)
public class EagerToOneWithJoinFetchModeTests implements SessionFactoryProducer {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	public SessionFactoryImplementor produceSessionFactory(MetadataImplementor model) {
		final SessionFactoryBuilder sessionFactoryBuilder = model.getSessionFactoryBuilder();
		sqlStatementInterceptor = new SQLStatementInterceptor( sessionFactoryBuilder );
		return (SessionFactoryImplementor) sessionFactoryBuilder.build();
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleEntity manyToOneSimpleEntity = new SimpleEntity( 1, "manyToOne" );
			SimpleEntity oneToOneSimpleEntity = new SimpleEntity( 2, "oneToOne" );
			session.save( manyToOneSimpleEntity );
			session.save( oneToOneSimpleEntity );

			RootEntity rootEntity = new RootEntity( 1, "root" );
			rootEntity.manyToOneSimpleEntity = manyToOneSimpleEntity;
			rootEntity.oneToOneSimpleEntity = oneToOneSimpleEntity;
			session.save( rootEntity );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "delete from RootEntity" ).executeUpdate();
			session.createQuery( "delete from SimpleEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.find( RootEntity.class, 1 );

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();

					assertThat( sqls.size(), is( 1 ) );
					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String executedStatement = sqls.get( 0 );
					assertThat( executedStatement, containsString( " root_entity " ) );
					assertThat( executedStatement, containsString( " left outer join simple_entity " ) );
					assertThat(
							executedStatement.replaceFirst( "left outer join", "" ),
							containsString( " left outer join " )
					);
					assertThat(
							executedStatement.replaceFirst( " left outer join", "" )
									.replaceFirst( "left outer join", "" ),
							not( containsString( " join " ) )
					);
				}
		);
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
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

					assertThat( sqls.get( 0 ), not( containsString( " join " ) ) );
					assertThat( sqls.get( 0 ), containsString( " root_entity " ) );

					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 1 ), not( containsString( " join " ) ) );

					assertThat( sqls.get( 2 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 2 ), not( containsString( " join " ) ) );
				}
		);
	}

	@Test
	public void testHqlJoinManyToOne(SessionFactoryScope scope) {
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
					assertThat( firstStatement, containsString( " inner join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat(
							firstStatement.replaceFirst( "inner join", "" ),
							not( containsString( " join " ) )
					);

					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 1 ), not( containsString( " join " ) ) );

					assertThat( sqls.get( 2 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 2 ), not( containsString( " join " ) ) );
				}
		);
	}

	@Test
	public void testHqlJoinOneToOne(SessionFactoryScope scope) {
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
					assertThat( firstStatement, containsString( " inner join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat(
							firstStatement.replaceFirst( "inner join", "" ),
							not( containsString( " join " ) )
					);

					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 1 ), not( containsString( " join " ) ) );

					assertThat( sqls.get( 2 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 2 ), not( containsString( " join " ) ) );
				}
		);
	}

	@Test
	public void testHqlJoinFetchManyToOne(SessionFactoryScope scope) {
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
					assertThat( firstStatement, containsString( " inner join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat( firstStatement, containsString( " join simple_entity " ) );
					assertThat(
							firstStatement.replaceFirst( "inner join", "" ),
							not( containsString( " join " ) )
					);

					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
				}
		);
	}

	@Test
	public void testHqlJoinFetchOneToOne(SessionFactoryScope scope) {
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
					assertThat( firstStatement, containsString( " inner join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat( firstStatement, containsString( " join simple_entity " ) );
					assertThat(
							firstStatement.replaceFirst( "inner join", "" ),
							not( containsString( " join " ) )
					);

					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
				}
		);
	}

	@Test
	public void testHqlJoinManyToOneAndOneToOne(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					sqlStatementInterceptor.clear();

					RootEntity rootEntity = session.createQuery(
							"select r from RootEntity r join r.manyToOneSimpleEntity join r.oneToOneSimpleEntity where r.id = :id",
							RootEntity.class
					).setParameter( "id", 1 ).getSingleResult();

					List<String> sqls = sqlStatementInterceptor.getSqlQueries();
					assertThat( sqls.size(), is( 3 ) );

					assertThat( Hibernate.isInitialized( rootEntity.manyToOneSimpleEntity ), is( true ) );
					assertThat( Hibernate.isInitialized( rootEntity.oneToOneSimpleEntity ), is( true ) );

					String firstStatement = sqls.get( 0 );
					assertThat( firstStatement, containsString( " inner join " ) );
					assertThat( firstStatement, containsString( " root_entity " ) );
					assertThat(
							firstStatement.replaceFirst( "inner join", "" ),
							containsString( " inner join " )
					);
					assertThat(
							firstStatement
									.replaceFirst( "inner join", "" )
									.replaceFirst( "inner join", "" )
							,
							not( containsString( " join " ) )
					);

					assertThat( sqls.get( 1 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 1 ), not( containsString( " join " ) ) );

					assertThat( sqls.get( 2 ), containsString( " simple_entity " ) );
					assertThat( sqls.get( 2 ), not( containsString( " join " ) ) );

				}
		);
	}

	@Entity(name = "RootEntity")
	@Table(name = "root_entity")
	public static class RootEntity {
		@Id
		private Integer id;
		private String name;

		@ManyToOne
		@Fetch(FetchMode.JOIN)
		private SimpleEntity manyToOneSimpleEntity;

		@OneToOne
		@Fetch(FetchMode.JOIN)
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
