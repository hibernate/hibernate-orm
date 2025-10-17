/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {EmbeddedIdTest.SystemUser.class})
public class EmbeddedIdTest {

	@BeforeEach
	public void init(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			SystemUser systemUser = new SystemUser();
			systemUser.setPk(new PK(
				"Hibernate Forum",
				"vlad"
			));
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

	//tag::identifiers-basic-embeddedid-mapping-example[]
	@Entity(name = "SystemUser")
	public static class SystemUser {

		@EmbeddedId
		private PK pk;

		private String name;

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-embeddedid-mapping-example[]

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
	//tag::identifiers-basic-embeddedid-mapping-example[]
	}

	@Embeddable
	public static class PK implements Serializable {

		private String subsystem;

		private String username;

		public PK(String subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		private PK() {
		}

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
	//end::identifiers-basic-embeddedid-mapping-example[]
}
