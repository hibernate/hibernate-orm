/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Luis Barreiro
 */
@BytecodeEnhanced
public class ManyToManyAssociationTest {

	@Test
	public void test() {
		Group group = new Group();
		Group anotherGroup = new Group();

		User user = new User();
		User anotherUser = new User();

		user.addGroup( group );
		user.addGroup( anotherGroup );
		anotherUser.addGroup( group );

		assertEquals( 2, group.getUsers().size() );
		assertEquals( 1, anotherGroup.getUsers().size() );

		group.resetUsers();

		assertEquals( 1, user.getGroups().size() );
		assertEquals( 0, anotherUser.getGroups().size() );

		// Test remove
		user.addGroup( group );
		anotherUser.addGroup( group );

		assertEquals( 2, group.getUsers().size() );
		assertEquals( 1, anotherGroup.getUsers().size() );

		Set<Group> groups = new HashSet<>( user.getGroups() );
		groups.remove( group );
		user.setGroups( groups );

		assertEquals( 1, group.getUsers().size() );
		assertEquals( 1, anotherGroup.getUsers().size() );

		groups.remove( anotherGroup );
		user.setGroups( groups );

		assertEquals( 1, group.getUsers().size() );
		// This happens (and is expected) because there was no snapshot taken before remove
		assertEquals( 1, anotherGroup.getUsers().size() );
	}

	// -- //

	@Entity
	private static class Group {

		@Id
		Long id;

		@Column
		String name;

		@ManyToMany( mappedBy = "groups" )
		Set<User> users = new HashSet<>();

		Set<User> getUsers() {
			return Collections.unmodifiableSet( users );
		}

		void resetUsers() {
			// this wouldn't trigger association management: users.clear();
			users = new HashSet<>();
		}
	}

	@Entity
	private static class User {

		@Id
		Long id;

		String password;

		@ManyToMany
		Set<Group> groups;

		void addGroup(Group group) {
			Set<Group> groups = this.groups == null ? new HashSet<>() : this.groups;
			groups.add( group );
			this.groups = groups;
		}

		Set<Group> getGroups() {
			return Collections.unmodifiableSet( groups );
		}

		void setGroups(Set<Group> groups) {
			this.groups = groups;
		}
	}
}
