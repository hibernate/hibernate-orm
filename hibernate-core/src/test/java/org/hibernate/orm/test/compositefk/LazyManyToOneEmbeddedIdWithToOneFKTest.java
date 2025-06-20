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
import jakarta.persistence.FetchType;
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
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				LazyManyToOneEmbeddedIdWithToOneFKTest.System.class,
				LazyManyToOneEmbeddedIdWithToOneFKTest.SystemUser.class,
				LazyManyToOneEmbeddedIdWithToOneFKTest.Subsystem.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class LazyManyToOneEmbeddedIdWithToOneFKTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Subsystem subsystem = new Subsystem( 2, "sub1" );
					PK userKey = new PK( subsystem, "Fab" );
					SystemUser user = new SystemUser( userKey, "Fab" );

					System system = new System( 1, "sub1" );
					system.setUser( user );

					session.persist( subsystem );
					session.persist( user );
					session.persist( system );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					System system = session.get( System.class, 1 );
					assertThat( system, is( notNullValue() ) );

					statementInspector.assertExecutedCount( 2 );

					assertThat( system.getId(), is( 1 ) );

					assertFalse( Hibernate.isInitialized( system.getUser() ) );

					PK pk = system.getUser().getPk();
					assertTrue( Hibernate.isInitialized( pk.subsystem ) );

					assertThat( pk.username, is( "Fab" ) );
					assertThat( pk.subsystem.id, is( 2 ) );
					assertThat( pk.subsystem.getDescription(), is( "sub1" ) );

					SystemUser user = system.getUser();
					assertThat( user, is( notNullValue() ) );

					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 0 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );

					statementInspector.clear();
					assertThat( user.getName(), is( "Fab" ) );
					assertTrue( Hibernate.isInitialized( system.getUser() ) );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );

				}
		);
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					System system = (System) session.createQuery( "from System e where e.id = :id" )
							.setParameter( "id", 1 ).uniqueResult();

					assertThat( system, is( notNullValue() ) );

					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 0 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );

					assertFalse( Hibernate.isInitialized( system.getUser() ) );

					final PK pk = system.getUser().getPk();
					assertTrue( Hibernate.isInitialized( pk.subsystem ) );

					assertThat( pk.username, is( "Fab" ) );
					assertThat( pk.subsystem.id, is( 2 ) );
					assertThat( pk.subsystem.getDescription(), is( "sub1" ) );

					SystemUser user = system.getUser();
					assertThat( user, is( notNullValue() ) );
					statementInspector.assertExecutedCount( 2 );

					statementInspector.clear();
					assertThat( user.getName(), is( "Fab" ) );
					assertTrue( Hibernate.isInitialized( system.getUser() ) );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );

				}
		);
	}

	@Test
	public void testHqlJoin(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					System system = session.createQuery( "from System e join e.user where e.id = :id", System.class )
							.setParameter( "id", 1 ).uniqueResult();
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );

					assertFalse( Hibernate.isInitialized( system.getUser() ) );

					assertThat( system, is( notNullValue() ) );
					SystemUser user = system.getUser();
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
							"from System e join fetch e.user where e.id = :id",
							System.class
					)
							.setParameter( "id", 1 ).uniqueResult();
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "join", 1 );
					statementInspector.assertNumberOfOccurrenceInQuery( 1, "join", 0 );

					assertTrue( Hibernate.isInitialized( system.getUser() ) );


					assertThat( system, is( notNullValue() ) );
					SystemUser user = system.getUser();
					assertThat( user, is( notNullValue() ) );
				}
		);
	}

	@Test
	public void testHql2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					// intentionally set the Subsystem description to "sub2", only the Subsystem.id value is used for the parameter binding
					Subsystem subsystem = new Subsystem( 2, "sub2" );
					PK userKey = new PK( subsystem, "Fab" );
					SystemUser systemUser = session.createQuery(
							"from SystemUser s where s.pk = :id",
							SystemUser.class
					).setParameter( "id", userKey ).uniqueResult();

					assertThat( systemUser.getPk().getSubsystem().getDescription(), is( "sub1" ) );
				}
		);
	}

	@Entity(name = "System")
	@Table( name = "systems" )
	public static class System {
		@Id
		private Integer id;
		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		SystemUser user;

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

		public SystemUser getUser() {
			return user;
		}

		public void setUser(SystemUser user) {
			this.user = user;
		}
	}

	@Entity(name = "SystemUser")
	public static class SystemUser {

		@EmbeddedId
		private PK pk;

		private String name;

		public SystemUser() {
		}

		public SystemUser(PK pk, String name) {
			this.pk = pk;
			this.name = name;
		}

		public PK getPk() {
			return pk;
		}

		public void setPk(PK pk) {
			this.pk = pk;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class PK implements Serializable {

		@ManyToOne
		private Subsystem subsystem;

		private String username;

		public PK(Subsystem subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		private PK() {
		}

		public Subsystem getSubsystem() {
			return subsystem;
		}

		public void setSubsystem(Subsystem subsystem) {
			this.subsystem = subsystem;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
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
			return Objects.equals( subsystem, pk.subsystem ) &&
					Objects.equals( username, pk.username );
		}

		@Override
		public int hashCode() {
			return Objects.hash( subsystem, username );
		}
	}

	@Entity(name = "Subsystem")
	public static class Subsystem {

		@Id
		private Integer id;

		private String description;

		public Subsystem() {
		}

		public Subsystem(Integer id, String description) {
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
}
