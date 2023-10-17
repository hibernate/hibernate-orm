/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ DirtyCheckEnhancementContext.class })
public class ManyToOneAccessByPropertyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyStatisticsSupport( true );
		sfb.applySecondLevelCacheSupport( false );
		sfb.applyQueryCacheSupport( false );
	}

	@Override
	protected void applyMetadataSources(MetadataSources sources) {
		super.applyMetadataSources( sources );
		sources.addAnnotatedClass( User.class );
		sources.addAnnotatedClass( Office.class );
		sources.addAnnotatedClass( InternalRequest.class );
	}

	private Long userId;
	private Long targetUserId;

	@Before
	public void setUp() {
		inTransaction(
				session -> {

					Office office = new Office( "The office" );

					session.persist( office );

					User user = new User();
					user.setOffice( office );
					user.setName( "Fab" );

					session.persist( user );

					userId = user.getId();

					user = new User();
					user.setOffice( office );
					user.setName( "And" );

					session.persist( user );
					targetUserId = user.getId();
				}
		);
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createQuery( "delete from InternalRequest" ).executeUpdate();
					session.createQuery( "delete from User" ).executeUpdate();
					session.createQuery( "delete from Office" ).executeUpdate();
				}
		);
	}

	@Test
	public void testPersist() {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		InternalRequest internalRequest = new InternalRequest( 1L );
		inTransaction(
				session -> {
					User user = session.find( User.class, userId );
					internalRequest.setUser( user );
					assertThat( stats.getPrepareStatementCount(), is( 1L ) );

					User targetUser = session.find( User.class, targetUserId );
					assertThat( stats.getPrepareStatementCount(), is( 2L ) );

					internalRequest.setTargetUser( targetUser );

					session.persist( internalRequest );

				}
		);
		assertThat( stats.getPrepareStatementCount(), is( 3L ) );
	}

	@Entity(name = "InternalRequest")
	public static class InternalRequest {

		private Long id;

		private User user;

		private User targetUser;

		InternalRequest() {
		}

		public InternalRequest(Long id) {
			this.id = id;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "idUser")
		public User getUser() {
			return user;
		}

		public void setUser(User targetUser) {
			this.user = targetUser;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "target_user")
		public User getTargetUser() {
			return targetUser;
		}

		public void setTargetUser(User targetUser) {
			this.targetUser = targetUser;
		}
	}

	@Entity(name = "User")
	@Table(name = "`User`")
	public static class User {

		private Long id;

		private String name;

		private Office office;

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "idOffice")
		public Office getOffice() {
			return office;
		}

		public void setOffice(Office office) {
			this.office = office;
		}
	}

	@Entity(name = "Office")
	public static class Office {

		private Long id;

		private String name;

		private Boolean isActive = true;

		Office() {
		}

		public Office(String name) {
			this.name = name;
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Boolean getActive() {
			return isActive;
		}

		public void setActive(Boolean active) {
			isActive = active;
		}
	}

}
