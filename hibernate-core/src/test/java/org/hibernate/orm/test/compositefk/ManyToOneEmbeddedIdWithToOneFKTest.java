/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				ManyToOneEmbeddedIdWithToOneFKTest.System.class,
				ManyToOneEmbeddedIdWithToOneFKTest.DataCenterUser.class,
				ManyToOneEmbeddedIdWithToOneFKTest.DataCenter.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class ManyToOneEmbeddedIdWithToOneFKTest {

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					final System system = session.get( System.class, 1 );
					assertThat( system, is( notNullValue() ) );
					assertThat( system.getId() , is(1 ) );

					assertThat( system.getDataCenterUser(), notNullValue() );
					assertThat( system.getDataCenterUser().getPk(), notNullValue() );
					assertTrue( Hibernate.isInitialized( system.getDataCenterUser() ) );

					final PK pk = system.getDataCenterUser().getPk();
					assertTrue( Hibernate.isInitialized( pk.dataCenter ) );

					assertThat( pk.username, is( "Fab" ) );
					assertThat( pk.dataCenter.id, is( 2 ) );
					assertThat( pk.dataCenter.getDescription(), is( "Raleigh" ) );

					final DataCenterUser user = system.getDataCenterUser();
					assertThat( user, notNullValue() );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 2 );
				}
		);
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					// this HQL should load the System with id = 1

					System system = (System) session.createQuery( "from System e where e.id = :id" )
							.setParameter( "id", 1 ).uniqueResult();

					assertThat( system, is( notNullValue() ) );

					statementInspector.assertExecutedCount( 3 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 0 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );
					statementInspector.assertNumberOfOccurrenceInQuery( 2, "join", 1 );


					assertTrue( Hibernate.isInitialized( system.getDataCenterUser() ) );

					final PK pk = system.getDataCenterUser().getPk();
					assertTrue( Hibernate.isInitialized( pk.dataCenter ) );

					assertThat( pk.username, is( "Fab" ) );
					assertThat( pk.dataCenter.id, is( 2 ) );
					assertThat( pk.dataCenter.getDescription(), is( "Raleigh" ) );

					DataCenterUser user = system.getDataCenterUser();
					assertThat( user, is( notNullValue() ) );
					statementInspector.assertExecutedCount( 3 );
				}
		);
	}

	@Test
	public void testHqlJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					System system = session.createQuery( "from System e join e.dataCenterUser where e.id = :id", System.class )
							.setParameter( "id", 1 ).uniqueResult();
					statementInspector.assertExecutedCount( 3 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );
					statementInspector.assertNumberOfOccurrenceInQuery( 2, "join", 1 );
					assertThat( system, is( notNullValue() ) );
					DataCenterUser user = system.getDataCenterUser();
					assertThat( user, is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testHqlJoinFetch(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					System system = session.createQuery(
							"from System e join fetch e.dataCenterUser where e.id = :id",
							System.class
					)
							.setParameter( "id", 1 ).uniqueResult();
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );
					assertThat( system, is( notNullValue() ) );
					DataCenterUser user = system.getDataCenterUser();
					assertThat( user, is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testEmbeddedIdParameter(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataCenter dataCenter = new DataCenter( 2, "sub1" );

					PK superUserKey = new PK( dataCenter, "Fab" );

					System system = session.createQuery(
							"from System e join fetch e.dataCenterUser u where u.id = :id",
							System.class
					).setParameter( "id", superUserKey ).uniqueResult();

					assertThat( system, is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testHql2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					DataCenterUser system = (DataCenterUser) session.createQuery( "from DataCenterUser " )
							.uniqueResult();
					assertThat( system, is( notNullValue() ) );

					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 0 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );

					assertTrue( Hibernate.isInitialized( system.getPk().dataCenter ) );

				}
		);
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					DataCenter dataCenter = new DataCenter( 2, "Raleigh" );
					PK userKey = new PK( dataCenter, "Fab" );
					DataCenterUser user = new DataCenterUser( userKey, (byte) 1 );

					System system = new System( 1, "QA" );
					system.setDataCenterUser( user );

					session.persist( dataCenter );
					session.persist( user );
					session.persist( system );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "DataCenter")
	@Table(name = "data_center" )
	public static class DataCenter {

		@Id
		private Integer id;

		private String description;

		public DataCenter() {
		}

		public DataCenter(Integer id, String description) {
			this.id = id;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

//		public Integer getId() {
//			return id;
//		}
	}

	@Entity(name = "System")
	@Table( name = "systems" )
	public static class System {
		@Id
		private Integer id;
		private String name;

		@ManyToOne
		DataCenterUser dataCenterUser;

		public System() {
		}

		public System(Integer id, String name) {
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

		public DataCenterUser getDataCenterUser() {
			return dataCenterUser;
		}

		public void setDataCenterUser(DataCenterUser dataCenterUser) {
			this.dataCenterUser = dataCenterUser;
		}
	}

	@Entity( name = "DataCenterUser" )
	@Table(name = "data_center_user" )
	public static class DataCenterUser {

		@EmbeddedId
		private PK pk;

		private byte privilegeMask;

		public DataCenterUser() {
		}

		public DataCenterUser(DataCenter dataCenter, String username, byte privilegeMask) {
			this( new PK( dataCenter, username ), privilegeMask );
		}

		public DataCenterUser(PK pk, byte privilegeMask) {
			this.pk = pk;
			this.privilegeMask = privilegeMask;
		}

		public PK getPk() {
			return pk;
		}

		public void setPk(PK pk) {
			this.pk = pk;
		}

//		public String getName() {
//			return name;
//		}

//		public void setName(String name) {
//			this.name = name;
//		}
	}

	@Embeddable
	public static class PK implements Serializable {

		@ManyToOne
		private DataCenter dataCenter;
		private String username;

		public PK(DataCenter dataCenter, String username) {
			this.dataCenter = dataCenter;
			this.username = username;
		}

		private PK() {
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PK pk = (PK) o;
			return Objects.equals( dataCenter, pk.dataCenter ) &&
					Objects.equals( username, pk.username );
		}

		@Override
		public int hashCode() {
			return Objects.hash( dataCenter, username );
		}
	}
}
