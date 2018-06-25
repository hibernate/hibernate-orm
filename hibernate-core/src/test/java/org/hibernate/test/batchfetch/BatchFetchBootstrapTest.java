package org.hibernate.test.batchfetch;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.MappedSuperclass;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class BatchFetchBootstrapTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
			JafSid.class, UserGroup.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "30");
	}

	@Override
	protected void buildSessionFactory() {
	}

	@Test
	public void test() {
		super.buildSessionFactory();
	}


	@MappedSuperclass
	public abstract static class DatabaseEntity {
		private int id;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

	}

	@Entity(name = "JafSid")
	public static class JafSid extends DatabaseEntity {

		private Set<UserGroup> groups = new LinkedHashSet<>();

		@ManyToMany(mappedBy = "members", fetch = FetchType.EAGER)
		public Set<UserGroup> getGroups() {
			return groups;
		}

		public void setGroups(Set<UserGroup> groups) {
			this.groups = groups;
		}
	}

	@Entity(name = "UserGroup")
	public static class UserGroup extends DatabaseEntity {

		private Set<JafSid> members = new LinkedHashSet<>();

		@ManyToMany
		public Set<JafSid> getMembers() {
			return members;
		}

		public void setMembers(Set<JafSid> members) {
			this.members = members;
		}
	}
}
