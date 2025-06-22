/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				SimpleDynamicUpdateTest.User.class,
				SimpleDynamicUpdateTest.Role.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "100"),
				@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true"),
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class, DirtyCheckEnhancementContext.class })
public class SimpleDynamicUpdateTest {

	@BeforeAll
	static void beforeAll() {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		assumeFalse( byteCodeProvider != null && !BytecodeProviderInitiator.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals(
				byteCodeProvider ) );
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user = new User();
					user.setId( 1 );
					user.setEmail( "not null string" );

					Address address = new Address();
					address.setState( "Texas" );

					user.setAddress( address );

					Role role = new Role();
					role.setId( 2 );
					role.setName( "manager" );

					user.setRole( role );

					session.persist( role );
					session.persist( user );
				}
		);
	}

	@Test
	public void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user = session.getReference( User.class, 1 );
					assertThat(
							user, instanceOf( PersistentAttributeInterceptable.class )
					);
					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) user;
					assertThat(
							interceptable.$$_hibernate_getInterceptor(),
							instanceOf( EnhancementAsProxyLazinessInterceptor.class )
					);
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setName( "abc" );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( "abc" ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setRole( null );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( "abc" ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
					assertThat( entity.getRole(), is( nullValue() ) );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setName( null );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( nullValue() ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setAddress( null );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( nullValue() ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
					assertThat( entity.getRole(), is( nullValue() ) );
					assertThat( entity.getAddress(), is( nullValue() ) );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					Role role = new Role();
					role.setId( 3 );
					role.setName( "user" );
					entity.setRole( role );
					session.persist( role );
				}
		);

		scope.inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( nullValue() ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
					assertThat( entity.getRole(), is( notNullValue() ) );
					assertThat( entity.getAddress(), is( nullValue() ) );
				}
		);
	}

	@Entity(name = "User")
	@Table(name = "appuser")
	@DynamicUpdate
	public static class User {
		@Id
		private Integer id;

		@NotNull
		private String email;

		private String name;

		private Address address;

		@ManyToOne
		private Role role;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public Role getRole() {
			return role;
		}

		public void setRole(Role role) {
			this.role = role;
		}
	}

	@Entity(name = "Role")
	@Table(name = "approle")
	@DynamicUpdate
	public static class Role {
		@Id
		private Integer id;

		@NotNull
		private String name;

		private String description;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

	@Embeddable
	public static class Address {
		private String street;
		private String state;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}
	}

}
