/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.mapping.identifier;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class IdClassManyToOneTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			SystemUser.class,
			Subsystem.class
		};
	}

	@Before
	public void init() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Subsystem subsystem = new Subsystem();
			subsystem.setId( "Hibernate Forum" );
			subsystem.setDescription( "Hibernate projects forum" );
			entityManager.persist( subsystem );

			SystemUser systemUser = new SystemUser();
			systemUser.setId( new PK(
				subsystem,
				"vlad"
			) );
			systemUser.setName( "Vlad Mihalcea" );

			entityManager.persist( systemUser );
		} );
	}


	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Subsystem subsystem = entityManager.find(
				Subsystem.class,
				"Hibernate Forum"
			);
			SystemUser systemUser = entityManager.find(
				SystemUser.class,
				new PK(
					subsystem,
					"vlad"
				)
			);

			assertEquals( "Vlad Mihalcea", systemUser.getName() );
		} );

	}

	//tag::identifiers-basic-idclass-manytoone-mapping-example[]
	@Entity(name = "SystemUser")
	@IdClass( PK.class )
	public static class SystemUser {

		@Id
		@ManyToOne(fetch = FetchType.LAZY)
		private Subsystem subsystem;

		@Id
		private String username;

		private String name;

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-idclass-manytoone-mapping-example[]

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

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	//tag::identifiers-basic-idclass-manytoone-mapping-example[]
	}

	@Entity(name = "Subsystem")
	public static class Subsystem {

		@Id
		private String id;

		private String description;

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-idclass-manytoone-mapping-example[]

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
	//tag::identifiers-basic-idclass-manytoone-mapping-example[]
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

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-idclass-manytoone-mapping-example[]

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
	//tag::identifiers-basic-idclass-manytoone-mapping-example[]
	}
	//end::identifiers-basic-idclass-manytoone-mapping-example[]
}
