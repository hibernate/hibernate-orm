/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class, DirtyCheckEnhancementContext.class })
public class EntityWithMutableAttributesTest extends BaseNonConfigCoreFunctionalTestCase {

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
					user.setDate( new Date() );
					user.setEmail( "not null string" );


					Role role = new Role();
					role.setId( 2 );
					role.setDate( new Date() );
					role.setName( "manager" );

					user.setRole( role );

					session.save( role );
					session.save( user );
				}
		);
	}

	@After
	public void tearDown() {
		if ( skipTest ) {
			return;
		}
		inTransaction(
				session -> {
					session.createQuery( "delete from User" ).executeUpdate();
					session.createQuery( "delete from Role" ).executeUpdate();
				}
		);
	}

	@Test
	public void testLoad() {
		if ( skipTest ) {
			return;
		}
		inTransaction(
				session -> {
					User user = session.load( User.class, 1 );
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
	public void testMutableAttributeIsUpdated() {
		if ( skipTest ) {
			return;
		}
		inTransaction(
				session -> {
					User user = session.load( User.class, 1 );
					user.getDate().setTime( 0 );
				}
		);

		inTransaction(
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
