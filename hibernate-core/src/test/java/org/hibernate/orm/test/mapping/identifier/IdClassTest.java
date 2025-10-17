/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {IdClassTest.SystemUser.class})
public class IdClassTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SystemUser systemUser = new SystemUser();
			systemUser.setId(
				new PK(
					"Hibernate Forum",
					"vlad"
				)
			);
			systemUser.setName("Vlad Mihalcea");

			entityManager.persist(systemUser);
		});
	}


	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SystemUser systemUser = entityManager.find(
				SystemUser.class,
				new PK(
					"Hibernate Forum",
					"vlad"
				)
			);

			assertEquals("Vlad Mihalcea", systemUser.getName());
		});

	}

	//tag::identifiers-basic-idclass-mapping-example[]
	@Entity(name = "SystemUser")
	@IdClass(PK.class)
	public static class SystemUser {

		@Id
		private String subsystem;

		@Id
		private String username;

		private String name;

		public PK getId() {
			return new PK(
				subsystem,
				username
			);
		}

		public void setId(PK id) {
			this.subsystem = id.getSubsystem();
			this.username = id.getUsername();
		}

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-idclass-mapping-example[]

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
	//tag::identifiers-basic-idclass-mapping-example[]
	}

	public static class PK implements Serializable {

		private String subsystem;

		private String username;

		public PK(String subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		private PK() {
		}

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-idclass-mapping-example[]

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

	//tag::identifiers-basic-idclass-mapping-example[]

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			PK pk = (PK) o;
			return Objects.equals(subsystem, pk.subsystem) &&
					Objects.equals(username, pk.username);
		}

		@Override
		public int hashCode() {
			return Objects.hash(subsystem, username);
		}
	}
	//end::identifiers-basic-idclass-mapping-example[]
}
