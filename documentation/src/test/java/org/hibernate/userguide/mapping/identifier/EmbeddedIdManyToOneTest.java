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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddedIdManyToOneTest extends BaseEntityManagerFunctionalTestCase {

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
			systemUser.setPk( new PK(
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

	//tag::identifiers-basic-embeddedid-manytoone-mapping-example[]
	@Entity(name = "SystemUser")
	public static class SystemUser {

		@EmbeddedId
		private PK pk;

		private String name;

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-embeddedid-manytoone-mapping-example[]

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
	//tag::identifiers-basic-embeddedid-manytoone-mapping-example[]
	}

	@Entity(name = "Subsystem")
	public static class Subsystem {

		@Id
		private String id;

		private String description;

		//Getters and setters are omitted for brevity
	//end::identifiers-basic-embeddedid-manytoone-mapping-example[]

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
	//tag::identifiers-basic-embeddedid-manytoone-mapping-example[]
	}

	@Embeddable
	public static class PK implements Serializable {

		@ManyToOne(fetch = FetchType.LAZY)
		private Subsystem subsystem;

		private String username;

		public PK(Subsystem subsystem, String username) {
			this.subsystem = subsystem;
			this.username = username;
		}

		private PK() {
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
	}
	//end::identifiers-basic-embeddedid-manytoone-mapping-example[]
}
