/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@JiraKey(value = "HHH-13959")
public class OneToOneJoinTableUniquenessTest extends BaseCoreFunctionalTestCase {
	File output;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Profile.class, PersonRole.class };
	}

	@Override
	protected void configure(Configuration configuration) {

		try {
			output = File.createTempFile( "update_script", ".sql" );
		}
		catch (IOException e) {
			e.printStackTrace();
			fail( e.getMessage() );
		}
		String value = output.toPath().toString();
		configuration.setProperty( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, value );
		configuration.setProperty( AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create" );
		configuration.setProperty( AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "create-drop" );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, "false" );
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from PersonRole" ).executeUpdate();
					session.createQuery( "delete from Profile" ).executeUpdate();
				}
		);
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testJoinTableColumnAreBothNotNull() throws Exception {
		List<String> commands = Files.readAllLines( output.toPath() );
		boolean isJoinTableCreated = false;
		for ( String command : commands ) {
			String lowerCaseCommand = command.toLowerCase();
			if ( lowerCaseCommand.contains( "create table profile_person_role" ) ) {
				isJoinTableCreated = true;
				assertTrue( lowerCaseCommand.contains( "personrole_roleid bigint not null" ) );
				assertTrue( lowerCaseCommand.contains( "id bigint not null" ) );
			}
		}
		assertTrue( "The Join table was not created", isJoinTableCreated );
	}

	@Test
	public void testPersistProfile() {
		inTransaction(
				session -> {
					Profile p = new Profile();
					session.persist( p );
				}
		);
	}

	@Test
	public void testPersistPersonRole() {
		inTransaction(
				session -> {
					PersonRole personRole = new PersonRole();
					session.persist( personRole );
				}
		);
	}

	@Test
	public void testPersistBothSameTime() {
		inTransaction(
				session -> {
					PersonRole personRole = new PersonRole();
					Profile profile = new Profile();
					profile.addPersonalRole( personRole );

					session.persist( personRole );
					session.persist( profile );
				}
		);
	}

	@Test
	public void testPersistBothAndAssociateLater() {
		inTransaction(
				session -> {
					PersonRole personRole = new PersonRole();
					Profile profile = new Profile();
					session.persist( personRole );

					session.persist( profile );

					session.flush();

					profile.addPersonalRole( personRole );
				}
		);
	}

	@Entity(name = "Profile")
	@Table(name = "profile")
	public class Profile {
		protected Long id;

		String name;

		PersonRole personRole;

		public Profile() {
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@OneToOne(fetch = FetchType.LAZY)
		@JoinTable(
				name = "profile_person_role",
				inverseJoinColumns = { @JoinColumn(unique = true, nullable = false) }
		)
		public PersonRole getPersonRole() {
			return this.personRole;
		}

		public void setPersonRole(PersonRole personRole) {
			this.personRole = personRole;
		}

		public void addPersonalRole(PersonRole personRole) {
			this.personRole = personRole;
			personRole.setProfile( this );
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "PersonRole")
	@Table(name = "person_role")
	public class PersonRole {
		Long roleId;

		String email;

		Profile profile;

		public PersonRole() {
		}

		@Id
		@Column(name = "roleid")
		@GeneratedValue
		public Long getRoleId() {
			return this.roleId;
		}

		public void setRoleId(Long roleId) {
			this.roleId = roleId;
		}

		@Column(name = "email")
		public String getEmail() {
			return this.email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		@OneToOne(mappedBy = "personRole", fetch = FetchType.LAZY)
		public Profile getProfile() {
			return this.profile;
		}

		public void setProfile(Profile profile) {
			this.profile = profile;
		}
	}
}
