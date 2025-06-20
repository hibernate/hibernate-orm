/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

import org.hibernate.Hibernate;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				IdClassWithEagerManyToOneTest.SystemUser.class,
				IdClassWithEagerManyToOneTest.Subsystem.class
		}
)
@SessionFactory(useCollectingStatementInspector = true)
public class IdClassWithEagerManyToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Subsystem subsystem = new Subsystem( "1", "Linux" );
					SystemUser systemUser = new SystemUser( subsystem, "admin", "Andrea" );
					session.persist( subsystem );
					session.persist( systemUser );

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
					PK pk = new PK( new Subsystem( "1", "Linux2" ), "admin" );
					SystemUser systemUser = session.get( SystemUser.class, pk );
					assertThat( systemUser.getName(), is( "Andrea" ) );
					Subsystem subsystem = systemUser.getSubsystem();
					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery(
							0,
							"join",
							1
					);
					statementInspector.clear();

					assertTrue( Hibernate.isInitialized( subsystem ) );

					assertThat( subsystem.getId(), is( "1" ) );
					assertThat( subsystem.getDescription(), is( "Linux" ) );
					assertThat( systemUser.getUsername(), is( "admin" ) );

					statementInspector.assertExecutedCount( 0 );
				}
		);
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					PK pk = new PK( new Subsystem( "1", "Linux2" ), "admin" );
					SystemUser systemUser = session.createQuery(
							"from SystemUser s where s.id = :id",
							SystemUser.class
					).setParameter( "id", pk ).getSingleResult();

					assertThat( systemUser.getName(), is( "Andrea" ) );

					Subsystem subsystem = systemUser.getSubsystem();

					assertTrue( Hibernate.isInitialized( subsystem ) );
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery(
							0,
							"join",
							0
					);
					statementInspector.assertNumberOfOccurrenceInQuery(
							1,
							"join",
							0
					);
					statementInspector.clear();

					assertThat( subsystem.getId(), is( "1" ) );
					assertThat( subsystem.getDescription(), is( "Linux" ) );
					assertThat( systemUser.getUsername(), is( "admin" ) );

					assertTrue( Hibernate.isInitialized( subsystem ) );

					statementInspector.assertExecutedCount( 0 );
				}
		);

		statementInspector.clear();
		scope.inTransaction(
				session -> {
					SystemUser systemUser = session.createQuery(
							"from SystemUser s where s.username = :username",
							SystemUser.class
					).setParameter( "username", "admin" ).getSingleResult();
					assertThat( systemUser.getName(), is( "Andrea" ) );

					Subsystem subsystem = systemUser.getSubsystem();

					assertTrue( Hibernate.isInitialized( subsystem ) );
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery(
							0,
							"join",
							0
					);
					statementInspector.assertNumberOfOccurrenceInQuery(
							1,
							"join",
							0
					);
					statementInspector.clear();

					assertThat( subsystem.getId(), is( "1" ) );
					assertThat( subsystem.getDescription(), is( "Linux" ) );
					assertThat( systemUser.getUsername(), is( "admin" ) );

					statementInspector.assertExecutedCount( 0 );
				}
		);
	}

	@Test
	public void testHql2(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					// intentionally set the Subsystem description to "Linux6", only the Subsystem.id value is used for the parameter binding

					PK pk = new PK( new Subsystem( "1", "Linux6" ), "admin" );
					SystemUser systemUser = session.createQuery(
							"from SystemUser s where s.id = :id",
							SystemUser.class
					).setParameter( "id", pk ).getSingleResult();

					assertThat( systemUser.getName(), is( "Andrea" ) );

					Subsystem subsystem = systemUser.getSubsystem();

					assertTrue( Hibernate.isInitialized( subsystem ) );
					statementInspector.assertExecutedCount( 2 );
					statementInspector.assertNumberOfOccurrenceInQuery(
							0,
							"join",
							0
					);
					statementInspector.assertNumberOfOccurrenceInQuery(
							1,
							"join",
							0
					);
					statementInspector.clear();

					assertThat( subsystem.getId(), is( "1" ) );
					assertThat( subsystem.getDescription(), is( "Linux" ) );
					assertThat( systemUser.getUsername(), is( "admin" ) );

					statementInspector.assertExecutedCount( 0 );
				}
		);
	}


	@Entity(name = "SystemUser")
	@IdClass(PK.class)
	public static class SystemUser {

		@Id
		@ManyToOne
		private Subsystem subsystem;

		@Id
		private String username;

		private String name;

		public SystemUser(Subsystem subsystem, String username, String name) {
			this.subsystem = subsystem;
			this.username = username;
			this.name = name;
		}

		private SystemUser() {
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

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Subsystem")
	public static class Subsystem {

		@Id
		private String id;

		private String description;

		public Subsystem(String id, String description) {
			this.id = id;
			this.description = description;
		}

		private Subsystem() {
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	public static class PK implements Serializable {

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
	}
}
