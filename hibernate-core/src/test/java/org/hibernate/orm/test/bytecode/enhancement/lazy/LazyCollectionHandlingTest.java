/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				LazyCollectionHandlingTest.JafSid.class, LazyCollectionHandlingTest.UserGroup.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyCollectionHandlingTest {

	private Integer id;

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			JafSid sid = new JafSid();
			s.persist( sid );

			s.flush();
			s.clear();

			this.id = sid.getId();
		});

		scope.inTransaction( s -> {
			s.get( JafSid.class, this.id );
		} );
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
