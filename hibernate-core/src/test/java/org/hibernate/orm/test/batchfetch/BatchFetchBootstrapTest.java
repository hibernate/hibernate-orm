/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batchfetch;

import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;


@DomainModel(
		annotatedClasses = {
				BatchFetchBootstrapTest.JafSid.class, BatchFetchBootstrapTest.UserGroup.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting( name =  AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "30")
)
public class BatchFetchBootstrapTest {

	@Test
	public void test() {
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
