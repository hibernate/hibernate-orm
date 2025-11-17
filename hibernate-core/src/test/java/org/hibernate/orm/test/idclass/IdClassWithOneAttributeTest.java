/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = IdClassWithOneAttributeTest.SystemUser.class
)
@SessionFactory
@JiraKey(value = "HHH-15286")
public class IdClassWithOneAttributeTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PK pk = new PK( "Linux" );
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
		scope.inTransaction(
				session -> {
					PK pk = new PK( "Linux" );
					SystemUser systemUser = session.get( SystemUser.class, pk );
					assertNotNull(systemUser);
				}
		);
	}

	@Entity(name = "SystemUser")
	@IdClass(PK.class)
	public static class SystemUser {

		@Id
		private String subsystem;

		private String name;

		public PK getId() {
			return new PK( subsystem );
		}

		public void setId(PK id) {
			this.subsystem = id.getSubsystem();
		}

		public String getSubsystem() {
			return subsystem;
		}

		public void setSubsystem(String subsystem) {
			this.subsystem = subsystem;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	public static class PK implements Serializable {

		private String subsystem;

		public PK(String subsystem) {
			this.subsystem = subsystem;
		}

		private PK() {
		}

		public String getSubsystem() {
			return subsystem;
		}

		public void setSubsystem(String subsystem) {
			this.subsystem = subsystem;
		}

	}
}
