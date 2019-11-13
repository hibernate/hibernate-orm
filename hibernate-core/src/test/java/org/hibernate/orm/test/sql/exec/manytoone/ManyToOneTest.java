/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import java.sql.Statement;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneTest.SimpleEntity.class,
				ManyToOneTest.OtherEntity.class,
				ManyToOneTest.AnotherSimpleEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class ManyToOneTest {

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {

					OtherEntity otherEntity = session.
							createQuery( "from OtherEntity", OtherEntity.class )
							.uniqueResult();

					assertThat( statistics.getPrepareStatementCount(), is(1L) );

					assertThat( otherEntity.getName(), is( "Bar" ) );
					SimpleEntity simpleEntity = otherEntity.getSimpleEntity();
					assertFalse( Hibernate.isInitialized( simpleEntity ) );

					AnotherSimpleEntity anotherSimpleEntity = otherEntity.getAnotherSimpleEntity();
					// the ManyToOne is eager but the value is null so a second query is not executed
					assertTrue( Hibernate.isInitialized( anotherSimpleEntity ) );

					assertThat( simpleEntity.getName(), is( "Fab" ) );
					assertThat( statistics.getPrepareStatementCount(), is(2L) );

					assertTrue( Hibernate.isInitialized( simpleEntity ) );
				}
		);

		scope.inTransaction(
				session -> {
					OtherEntity otherEntity = session.
							createQuery( "from OtherEntity", OtherEntity.class )
							.uniqueResult();
					AnotherSimpleEntity anotherSimpleEntity = new AnotherSimpleEntity();
					anotherSimpleEntity.setId( 3 );
					anotherSimpleEntity.setName( "other" );
					session.save( anotherSimpleEntity );
					otherEntity.setAnotherSimpleEntity( anotherSimpleEntity );
				}
		);

		statistics.clear();

		scope.inTransaction(
				session -> {
					OtherEntity otherEntity = session.
							createQuery( "from OtherEntity", OtherEntity.class )
							.uniqueResult();
					// the ManyToOne is eager but the value is not null so a second query is executed
					assertThat( statistics.getPrepareStatementCount(), is(2L) );

					assertThat( otherEntity.getName(), is( "Bar" ) );
					SimpleEntity simpleEntity = otherEntity.getSimpleEntity();
					assertFalse( Hibernate.isInitialized( simpleEntity ) );

					AnotherSimpleEntity anotherSimpleEntity = otherEntity.getAnotherSimpleEntity();
					assertTrue( Hibernate.isInitialized( anotherSimpleEntity ) );
					assertThat( statistics.getPrepareStatementCount(), is(2L) );
				}
		);
	}

	@Test
	public void testHQLSelectWithFetchJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					OtherEntity otherEntity = session.
							createQuery( "from OtherEntity o join fetch o.simpleEntity", OtherEntity.class )
							.uniqueResult();
					assertThat( statistics.getPrepareStatementCount(), is(1L) );

					assertThat( otherEntity.getName(), is( "Bar" ) );
					assertTrue( Hibernate.isInitialized( otherEntity.getSimpleEntity() ) );
					assertThat( otherEntity.getSimpleEntity(), notNullValue() );
					assertThat( otherEntity.getSimpleEntity().getName(), is( "Fab" ) );
					AnotherSimpleEntity anotherSimpleEntity = otherEntity.getAnotherSimpleEntity();
					assertTrue( Hibernate.isInitialized( anotherSimpleEntity ) );
					assertThat( statistics.getPrepareStatementCount(), is(1L) );

				}
		);
	}

	@Test
	public void testSelectWithBothFetchJoin(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					OtherEntity otherEntity = session.
							createQuery(
									"from OtherEntity o join fetch o.simpleEntity left join fetch o.anotherSimpleEntity",
									OtherEntity.class
							)
							.uniqueResult();
					assertThat( statistics.getPrepareStatementCount(), is(1L) );

					assertThat( otherEntity.getName(), is( "Bar" ) );
					assertTrue( Hibernate.isInitialized( otherEntity.getSimpleEntity() ) );
					assertThat( otherEntity.getSimpleEntity(), notNullValue() );
					assertThat( otherEntity.getSimpleEntity().getName(), is( "Fab" ) );
					assertTrue( Hibernate.isInitialized( otherEntity.getAnotherSimpleEntity() ) );
					assertThat( otherEntity.getAnotherSimpleEntity(), nullValue() );
					assertThat( statistics.getPrepareStatementCount(), is(1L) );
				}
		);

		scope.inTransaction(
				session -> {
					OtherEntity otherEntity = session.
							createQuery( "from OtherEntity", OtherEntity.class )
							.uniqueResult();
					AnotherSimpleEntity anotherSimpleEntity = new AnotherSimpleEntity();
					anotherSimpleEntity.setId( 3 );
					anotherSimpleEntity.setName( "other" );
					session.save( anotherSimpleEntity );
					otherEntity.setAnotherSimpleEntity( anotherSimpleEntity );
				}
		);

		statistics.clear();

		scope.inTransaction(
				session -> {
					OtherEntity otherEntity = session.
							createQuery( "from OtherEntity o join fetch o.simpleEntity left join fetch o.anotherSimpleEntity", OtherEntity.class )
							.uniqueResult();
					// the ManyToOne is eager but the value is not null so a second query is executed
					assertThat( statistics.getPrepareStatementCount(), is(1L) );

					assertThat( otherEntity.getName(), is( "Bar" ) );
					SimpleEntity simpleEntity = otherEntity.getSimpleEntity();
					assertTrue( Hibernate.isInitialized( simpleEntity ) );

					AnotherSimpleEntity anotherSimpleEntity = otherEntity.getAnotherSimpleEntity();
					assertTrue( Hibernate.isInitialized( anotherSimpleEntity ) );
					assertThat( statistics.getPrepareStatementCount(), is(1L) );
				}
		);
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					OtherEntity otherEntity = session.get( OtherEntity.class, 2 );

					assertThat( otherEntity.getName(), is( "Bar" ) );
					assertFalse( Hibernate.isInitialized( otherEntity.getSimpleEntity() ) );
					assertTrue( Hibernate.isInitialized( otherEntity.getAnotherSimpleEntity() ) );
					assertThat( statistics.getPrepareStatementCount(), is(1L) );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.remove( session.get( OtherEntity.class, 2 ) );
				}

		);
		scope.inTransaction(
				session -> {
					assertThat( session.get( OtherEntity.class, 2 ), nullValue() );
					assertThat( session.get( SimpleEntity.class, 1 ), notNullValue() );
				}
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SimpleEntity simpleEntity = new SimpleEntity();
					simpleEntity.setId( 1 );
					simpleEntity.setName( "Fab" );
					session.save( simpleEntity );
					OtherEntity otherEntity = new OtherEntity();
					otherEntity.setId( 2 );
					otherEntity.setName( "Bar" );
					otherEntity.setSimpleEntity( simpleEntity );
					session.save( otherEntity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.doWork(
								work -> {
									Statement statement = work.createStatement();
									statement.execute( "delete from mapping_other_entity" );
									statement.execute( "delete from mapping_another_simple_entity" );
									statement.execute( "delete from mapping_simple_entity" );
									statement.close();
								}
						)
		);
	}

	@Entity(name = "OtherEntity")
	@Table(name = "mapping_other_entity")
	public static class OtherEntity {
		private Integer id;
		private String name;

		private SimpleEntity simpleEntity;

		private AnotherSimpleEntity anotherSimpleEntity;

		@Id
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

		@ManyToOne(fetch = FetchType.LAZY)
		public SimpleEntity getSimpleEntity() {
			return simpleEntity;
		}

		public void setSimpleEntity(SimpleEntity simpleEntity) {
			this.simpleEntity = simpleEntity;
		}

		@ManyToOne
		public AnotherSimpleEntity getAnotherSimpleEntity() {
			return anotherSimpleEntity;
		}

		public void setAnotherSimpleEntity(AnotherSimpleEntity anotherSimpleEntity) {
			this.anotherSimpleEntity = anotherSimpleEntity;
		}
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "mapping_simple_entity")
	public static class SimpleEntity {
		private Integer id;
		private String name;

		@Id
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

	@Entity(name = "AnotherSimpleEntity")
	@Table(name = "mapping_another_simple_entity")
	public static class AnotherSimpleEntity {
		private Integer id;
		private String name;

		@Id
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
