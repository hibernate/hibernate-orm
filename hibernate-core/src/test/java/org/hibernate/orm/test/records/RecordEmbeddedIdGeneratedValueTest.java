/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

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
		RecordEmbeddedIdGeneratedValueTest.SystemUser.class,
		RecordEmbeddedIdGeneratedValueTest.PK.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-15074" )
public class RecordEmbeddedIdGeneratedValueTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from SystemUser" ).executeUpdate() );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		final SystemUser _systemUser = scope.fromTransaction( session -> {
			final SystemUser systemUser = new SystemUser();
			systemUser.setId( new PK( "mbladel", null ) );
			systemUser.setName( "Marco Belladelli" );
			session.persist( systemUser );
			return systemUser;
		} );

		scope.inSession( session -> {
			final SystemUser systemUser = session.find( SystemUser.class, _systemUser.getId() );
			assertThat( systemUser.getName() ).isEqualTo( "Marco Belladelli" );
			assertThat( systemUser.getId().username() ).isEqualTo( "mbladel" );
			assertThat( systemUser.getId().registrationId() ).isNotNull();
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
	public record PK(String username, @GeneratedValue Integer registrationId) {}
}
