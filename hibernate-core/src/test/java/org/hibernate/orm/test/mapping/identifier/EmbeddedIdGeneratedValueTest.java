/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddedIdGeneratedValueTest.SystemUser.class,
		EmbeddedIdGeneratedValueTest.PK.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-15074" )
public class EmbeddedIdGeneratedValueTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SystemUser" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final SystemUser _systemUser = scope.fromTransaction( session -> {
			final SystemUser systemUser = new SystemUser();
			systemUser.setId( new PK( "mbladel" ) );
			systemUser.setName( "Marco Belladelli" );
			session.persist( systemUser );
			return systemUser;
		} );

		scope.inSession( session -> {
			final SystemUser systemUser = session.find( SystemUser.class, _systemUser.getId() );
			assertThat( systemUser.getName() ).isEqualTo( "Marco Belladelli" );
			assertThat( systemUser.getId().getUsername() ).isEqualTo( "mbladel" );
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
	public static class PK implements Serializable {
		private String username;

		@GeneratedValue
		private Integer registrationId;

		public PK() {
		}

		public PK(String username) {
			this.username = username;
		}

		public String getUsername() {
			return username;
		}

		public Integer getRegistrationId() {
			return registrationId;
		}
	}
}
