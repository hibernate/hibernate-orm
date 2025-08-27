/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = { IdClassTest.SystemUser.class }
)
@SessionFactory(useCollectingStatementInspector = true)
public class IdClassTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PK pk = new PK( "Linux", "admin", 1 );
					SystemUser systemUser = new SystemUser();
					systemUser.setId( pk );
					systemUser.setName( "Andrea" );
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
					PK pk = new PK( "Linux", "admin", 1 );
					SystemUser systemUser = session.get( SystemUser.class, pk );
					assertThat( systemUser.getName(), is( "Andrea" ) );
					assertThat( systemUser.getSubsystem(), is( "Linux" ) );
					assertThat( systemUser.getUsername(), is( "admin" ) );
					assertThat( systemUser.getRegistrationId(), is( 1 ) );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery(
							0,
							"join",
							0
					);
				}
		);
	}

	@Test
	public void testHql(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					PK pk = new PK( "Linux", "admin", 1 );
					SystemUser systemUser = session.createQuery(
							"from SystemUser s where s.id = :id",
							SystemUser.class
					).setParameter( "id", pk ).getSingleResult();
					assertThat( systemUser.getName(), is( "Andrea" ) );
					assertThat( systemUser.getSubsystem(), is( "Linux" ) );
					assertThat( systemUser.getUsername(), is( "admin" ) );
					assertThat( systemUser.getRegistrationId(), is( 1 ) );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery(
							0,
							"join",
							0
					);
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
					assertThat( systemUser.getSubsystem(), is( "Linux" ) );
					assertThat( systemUser.getUsername(), is( "admin" ) );
					assertThat( systemUser.getRegistrationId(), is( 1 ) );

					statementInspector.assertExecutedCount( 1 );
					statementInspector.assertNumberOfOccurrenceInQuery(
							0,
							"join",
							0
					);
				}
		);
	}

	@Entity(name = "SystemUser")
	@IdClass(PK.class)
	public static class SystemUser {

		@Id
		private String subsystem;

		@Id
		private String username;

		@Id
		private Integer registrationId;

		private String name;

		public PK getId() {
			return new PK(
					subsystem,
					username,
					registrationId
			);
		}

		public void setId(PK id) {
			this.subsystem = id.getSubsystem();
			this.username = id.getUsername();
			this.registrationId = id.getRegistrationId();
		}

		public String getSubsystem() {
			return subsystem;
		}

		public void setSubsystem(String subsystem) {
			this.subsystem = subsystem;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Integer getRegistrationId() {
			return registrationId;
		}

		public void setRegistrationId(Integer registrationId) {
			this.registrationId = registrationId;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		//Getters and setters are omitted for brevity
	}

	public static class PK implements Serializable {

		private String subsystem;

		private String username;

		private Integer registrationId;

		public PK(String subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		public PK(String subsystem, String username, Integer registrationId) {
			this.subsystem = subsystem;
			this.username = username;
			this.registrationId = registrationId;
		}

		private PK() {
		}

		public String getSubsystem() {
			return subsystem;
		}

		public void setSubsystem(String subsystem) {
			this.subsystem = subsystem;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public Integer getRegistrationId() {
			return registrationId;
		}

		public void setRegistrationId(Integer registrationId) {
			this.registrationId = registrationId;
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
					Objects.equals( username, pk.username ) &&
					Objects.equals( registrationId, pk.registrationId );
		}

		@Override
		public int hashCode() {
			return Objects.hash( subsystem, username, registrationId );
		}
	}

}
