/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class, DirtyCheckEnhancementContext.class })
public class SimpleDynamicUpdateTest extends BaseNonConfigCoreFunctionalTestCase {

	boolean skipTest;

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "100" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		if ( byteCodeProvider != null && !Environment.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals( byteCodeProvider ) ) {
			// skip the test if the bytecode provider is Javassist
			skipTest = true;
		}
		else {
			sources.addAnnotatedClass( User.class );
			sources.addAnnotatedClass( Role.class );
		}
	}

	@Before
	public void setUp() {
		if ( skipTest ) {
			return;
		}
		inTransaction(
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

					session.save( role );
					session.save( user );
				}
		);
	}

	@Test
	public void testIt() {
		if ( skipTest ) {
			return;
		}
		inTransaction(
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

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setName( "abc" );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( "abc" ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setRole( null );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( "abc" ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
					assertThat( entity.getRole(), is( nullValue() ) );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setName( null );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( nullValue() ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					entity.setAddress( null );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					assertThat( entity.getName(), is( nullValue() ) );
					assertThat( entity.getEmail(), is( "not null string" ) );
					assertThat( entity.getRole(), is( nullValue() ) );
					assertThat( entity.getAddress(), is( nullValue() ) );
				}
		);

		inTransaction(
				session -> {
					User entity = session.getReference( User.class, 1 );
					Role role = new Role();
					role.setId( 3 );
					role.setName( "user" );
					entity.setRole( role );
					session.save( role );
				}
		);

		inTransaction(
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

