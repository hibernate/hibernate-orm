/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DomainModel(
		annotatedClasses = {
				EntityWithMutableAttributesTest.User.class,
				EntityWithMutableAttributesTest.Role.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "100" ),
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class, DirtyCheckEnhancementContext.class })
public class EntityWithMutableAttributesTest {

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
					user.setDate( new Date() );
					user.setEmail( "not null string" );


					Role role = new Role();
					role.setId( 2 );
					role.setDate( new Date() );
					role.setName( "manager" );

					user.setRole( role );

					session.persist( role );
					session.persist( user );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testLoad(SessionFactoryScope scope) {
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
	}

	@Test
	public void testMutableAttributeIsUpdated(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					User user = session.getReference( User.class, 1 );
					user.getDate().setTime( 0 );
				}
		);

		scope.inTransaction(
				session -> {
					User user = session.getReference( User.class, 1 );
					assertThat( user.getDate().getTime(), is( 0L ) );
				}
		);
	}


	@Entity(name = "User")
	@Table(name = "appuser")
	public static class User {
		@Id
		private Integer id;

		@NotNull
		private String email;

		private String name;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "t_date")
		private Date date;

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

		public Role getRole() {
			return role;
		}

		public void setRole(Role role) {
			this.role = role;
		}

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

	@Entity(name = "Role")
	@Table(name = "approle")
	public static class Role {
		@Id
		private Integer id;

		@NotNull
		private String name;

		@Temporal(TemporalType.TIMESTAMP)
		@Column(name = "t_date")
		private Date date;

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

		public Date getDate() {
			return date;
		}

		public void setDate(Date date) {
			this.date = date;
		}
	}

}
