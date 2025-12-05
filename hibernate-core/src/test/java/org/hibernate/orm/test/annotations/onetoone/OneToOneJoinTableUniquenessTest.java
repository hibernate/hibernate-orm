/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

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
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-13959")
@DomainModel(
		annotatedClasses = {
				OneToOneJoinTableUniquenessTest.Profile.class,
				OneToOneJoinTableUniquenessTest.PersonRole.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, value = "create"),
				@Setting(name = AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, value = "create-drop"),
				@Setting(name = AvailableSettings.FORMAT_SQL, value = "false")
		},
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET,
				provider = OneToOneJoinTableUniquenessTest.ScriptCreateTargetProvider.class
		)
)
public class OneToOneJoinTableUniquenessTest {

	public static class ScriptCreateTargetProvider implements SettingProvider.Provider<String> {
		static Path path;

		@Override
		public String getSetting() {
			try {
				File output = File.createTempFile( "update_script", ".sql" );
				path = output.toPath();
				return path.toString();
			}
			catch (IOException e) {
				throw new RuntimeException( e );
			}
		}
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	@RequiresDialect(H2Dialect.class)
	public void testJoinTableColumnAreBothNotNull(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory();
		List<String> commands = Files.readAllLines( ScriptCreateTargetProvider.path );
		boolean isJoinTableCreated = false;
		for ( String command : commands ) {
			String lowerCaseCommand = command.toLowerCase();
			if ( lowerCaseCommand.contains( "create table profile_person_role" ) ) {
				isJoinTableCreated = true;
				assertThat( lowerCaseCommand ).contains( "personrole_roleid bigint not null" );
				assertThat( lowerCaseCommand ).contains( "id bigint not null" );
			}
		}
		assertThat( isJoinTableCreated )
				.describedAs( "The Join table was not created" )
				.isTrue();
	}

	@Test
	public void testPersistProfile(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Profile p = new Profile();
					session.persist( p );
				}
		);
	}

	@Test
	public void testPersistPersonRole(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					PersonRole personRole = new PersonRole();
					session.persist( personRole );
				}
		);
	}

	@Test
	public void testPersistBothSameTime(SessionFactoryScope scope) {
		scope.inTransaction(
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
	public void testPersistBothAndAssociateLater(SessionFactoryScope scope) {
		scope.inTransaction(
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
				inverseJoinColumns = {@JoinColumn(unique = true, nullable = false)}
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
