/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ToOneSingleTableInheritanceAutoFlushTest.SiteUser.class,
		ToOneSingleTableInheritanceAutoFlushTest.Profile.class,
		ToOneSingleTableInheritanceAutoFlushTest.CommunityProfile.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
@Jira( "https://hibernate.atlassian.net/browse/HHH-17686" )
public class ToOneSingleTableInheritanceAutoFlushTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Profile profile = new Profile( 1L, "profile_1" );
			session.persist( profile );
			final CommunityProfile communityProfile = new CommunityProfile( 2L, "community_2", "community_2" );
			session.persist( communityProfile );
			final SiteUser siteUser = new SiteUser();
			siteUser.setProfile( profile );
			siteUser.setCommunityProfile( communityProfile );
			session.persist( siteUser );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from SiteUser" ).executeUpdate();
			session.createMutationQuery( "delete from Profile" ).executeUpdate();
		} );
	}

	@Test
	public void testSupertypeAutoFlush(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final Profile profile = session.find( Profile.class, 1L );
			inspector.clear();

			profile.setName( "new_profile_1" );
			final List<SiteUser> resultList = session.createQuery(
					"from SiteUser u join u.profile p where p.name = 'new_profile_1'",
					SiteUser.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getProfile().getName() ).isEqualTo( "new_profile_1" );
			inspector.assertIsUpdate( 0 );
			inspector.assertIsSelect( 1 );
		} );
	}

	@Test
	public void testSubtypeAutoFlush(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final CommunityProfile communityProfile = session.find( CommunityProfile.class, 2L );
			inspector.clear();

			communityProfile.setCommunity( "new_community_2" );
			final List<SiteUser> resultList = session.createQuery(
					"from SiteUser u join u.communityProfile p where p.community = 'new_community_2'",
					SiteUser.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ).getCommunityProfile().getCommunity() ).isEqualTo( "new_community_2" );
			inspector.assertIsUpdate( 0 );
			inspector.assertIsSelect( 1 );
		} );
	}

	@Entity( name = "SiteUser" )
	public static class SiteUser {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Profile profile;

		@OneToOne
		private CommunityProfile communityProfile;

		public Profile getProfile() {
			return profile;
		}

		public void setProfile(Profile profile) {
			this.profile = profile;
		}

		public CommunityProfile getCommunityProfile() {
			return communityProfile;
		}

		public void setCommunityProfile(CommunityProfile communityProfile) {
			this.communityProfile = communityProfile;
		}
	}

	@Entity( name = "Profile" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col" )
	@DiscriminatorValue( "profile" )
	public static class Profile {
		@Id
		private Long id;

		private String name;

		public Profile() {
		}

		public Profile(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String profileName) {
			this.name = profileName;
		}
	}

	@Entity( name = "CommunityProfile" )
	@DiscriminatorValue( "community" )
	public static class CommunityProfile extends Profile {
		private String community;

		public CommunityProfile() {
		}

		public CommunityProfile(Long id, String name, String community) {
			super( id, name );
			this.community = community;
		}

		public String getCommunity() {
			return community;
		}

		public void setCommunity(String community) {
			this.community = community;
		}
	}
}
