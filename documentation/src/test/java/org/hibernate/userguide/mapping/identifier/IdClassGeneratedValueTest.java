/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.identifier;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class IdClassGeneratedValueTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			SystemUser.class
		};
	}

	@Test
	public void test() {
		SystemUser _systemUser = doInJPA( this::entityManagerFactory, entityManager -> {
			SystemUser systemUser = new SystemUser();
			systemUser.setId(
					new PK(
							"Hibernate Forum",
							"vlad"
					)
			);
			systemUser.setName( "Vlad Mihalcea" );

			entityManager.persist( systemUser );

			return systemUser;
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			SystemUser systemUser = entityManager.find(
				SystemUser.class,
				_systemUser.getId()
			);

			assertEquals( "Vlad Mihalcea", systemUser.getName() );
		} );

	}

	//tag::identifiers-basic-idclass-generatedvalue-mapping-example[]
	@Entity(name = "SystemUser")
	@IdClass( PK.class )
	public static class SystemUser {

		@Id
		private String subsystem;

		@Id
		private String username;

		@Id
		@GeneratedValue
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

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-idclass-generatedvalue-mapping-example[]

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
	//tag::identifiers-basic-idclass-generatedvalue-mapping-example[]
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

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-idclass-generatedvalue-mapping-example[]

		public String getSubsystem() {
			return subsystem;
		}

		public String getUsername() {
			return username;
		}

		public Integer getRegistrationId() {
			return registrationId;
		}

		//tag::identifiers-basic-idclass-generatedvalue-mapping-example[]

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
	//end::identifiers-basic-idclass-generatedvalue-mapping-example[]
}
