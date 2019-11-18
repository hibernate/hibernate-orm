/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.junit.Test;

/**
 * @author Markus Lutum
 */
public class JtaLazyLoadingEMClosedSessionTest extends BaseEntityManagerFunctionalTestCase {

	private Long profileID;

	@Override
	protected void addConfigOptions(Map config) {
		super.addConfigOptions(config);
		config.put(AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true");

		TestingJtaBootstrap.prepare(config);
		config.put(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta");
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Profile.class, ProfileEntry.class, ProfileAuthorization.class };
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		super.afterEntityManagerFactoryBuilt();

		doInJPA(this::entityManagerFactory, entityManager -> {

			Profile profile = new Profile();
			// create and add one entry
			ProfileEntry profileEntry = profile.makeProfileEntry();
			// create and assign one authorization
			entityManager.persist(profileEntry.makeProfileAuthorization());
			entityManager.persist(profile);
			profileID = profile.getId();
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12210")
	public void testByteBuddySessionClosedIssue() {

		final List<Profile> loadedProfiles = new ArrayList<Profile>();

		doInJPA(this::entityManagerFactory, entityManager -> {
			Profile loadedProfile2 = (Profile) entityManager.find(Profile.class, profileID);
			loadedProfiles.add(loadedProfile2);
		});
		Profile loadedProfile = loadedProfiles.get(0);

		doInJPA(this::entityManagerFactory, entityManager -> {
			// initialize list and get first entry
			ProfileEntry profileEntry = loadedProfile.getProfileEntries().iterator().next();

			// try to get property from authorization
			long value = profileEntry.getProfileAuthorization().getPropertyC();
			assertTrue(value != 0);
		});
	}

	@Entity
	public static class Profile {
		private Long id;

		private List<ProfileEntry> profileEntries = new ArrayList<>();

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@OneToMany(mappedBy = "profile", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true)
		public List<ProfileEntry> getProfileEntries() {
			return profileEntries;
		}

		public void setProfileEntries(List<ProfileEntry> profileEntries) {
			this.profileEntries = profileEntries;
		}

		ProfileEntry makeProfileEntry() {
			final ProfileEntry pe = new ProfileEntry();
			pe.setProfile(this);
			this.profileEntries.add(pe);
			return pe;
		}
	}

	@Entity
	public static class ProfileEntry {

		private Long id;

		private Profile profile;
		private ProfileAuthorization profileAuthorization;

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		//@JoinColumn(name = "templateobjectid")
		public ProfileAuthorization getProfileAuthorization() {
			return profileAuthorization;
		}

		public void setProfileAuthorization(ProfileAuthorization profileAuthorization) {
			this.profileAuthorization = profileAuthorization;
		}

		@ManyToOne
		@JoinColumn(name = "profileobjectid", nullable = false)
		public Profile getProfile() {
			return profile;
		}

		public void setProfile(Profile profile) {
			this.profile = profile;
		}

		ProfileAuthorization makeProfileAuthorization() {
			this.profileAuthorization = new ProfileAuthorization();
			return profileAuthorization;
		}
	}

	@Entity
	public static class ProfileAuthorization {

		private Long id;
		// some properties
		private Long propertyC = new Random().nextLong();

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getPropertyC() {
			return propertyC;
		}

		public void setPropertyC(Long propertyC) {
			this.propertyC = propertyC;
		}
	}
}
