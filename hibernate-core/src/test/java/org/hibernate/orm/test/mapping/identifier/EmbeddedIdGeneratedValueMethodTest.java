/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;

import jakarta.persistence.Access;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;

import static jakarta.persistence.AccessType.PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Philippe Marschall
 */
@DomainModel( annotatedClasses = EmbeddedIdGeneratedValueMethodTest.SystemUser.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-20634" )
public class EmbeddedIdGeneratedValueMethodTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SystemUser" ).execute() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		String userName = "Philippe Marschall";
		String userId = "pmarschall";
		final SystemUser _systemUser = scope.fromTransaction( session -> {
			final SystemUser systemUser = new SystemUser();
			systemUser.setId( new PK( userId ) );
			systemUser.setName( userName );
			session.persist( systemUser );
			return systemUser;
		} );

		scope.inSession( session -> {
			final SystemUser systemUser = session.find( SystemUser.class, _systemUser.getId() );
			assertThat( systemUser.getName() ).isEqualTo( userName );
			assertThat( systemUser.getId().getUsername() ).isEqualTo( userId );
			assertThat( systemUser.getId().getRegistrationId() ).isNotNull();
		} );
	}

	@Entity( name = "SystemUser" )
	public static class SystemUser {
		@EmbeddedId
		private PK id;

		private String name;

		public PK getId() {
			return id;
		}

		public void setId(PK id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	@Access(PROPERTY)
	public static class PK implements Serializable {
		private String username;

		private Integer registrationId;

		public PK() {
		}

		public PK(String username) {
			this.username = username;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		@GeneratedValue
		public Integer getRegistrationId() {
			return registrationId;
		}

		public void setRegistrationId(Integer registrationId) {
			this.registrationId = registrationId;
		}
	}
}
