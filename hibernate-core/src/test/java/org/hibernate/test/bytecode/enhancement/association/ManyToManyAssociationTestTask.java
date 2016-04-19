/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class ManyToManyAssociationTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Group.class, User.class};
	}

	public void prepare() {
	}

	public void execute() {
		Group group = new Group();
		Group anotherGroup = new Group();

		User user = new User();
		User anotherUser = new User();

		user.addGroup( group );
		user.addGroup( anotherGroup );
		anotherUser.addGroup( group );

		Assert.assertTrue( group.getUsers().size() == 2 );
		Assert.assertTrue( anotherGroup.getUsers().size() == 1 );

		group.setUsers( new HashSet<User>() );

		Assert.assertTrue( user.getGroups().size() == 1 );
		Assert.assertTrue( anotherUser.getGroups().size() == 0 );

		// Test remove
		user.addGroup( group );
		anotherUser.addGroup( group );

		Assert.assertTrue( group.getUsers().size() == 2 );
		Assert.assertTrue( anotherGroup.getUsers().size() == 1 );

		Set<Group> groups = new HashSet<Group>( user.getGroups() );
		groups.remove( group );
		user.setGroups( groups );

		Assert.assertTrue( group.getUsers().size() == 1 );
		Assert.assertTrue( anotherGroup.getUsers().size() == 1 );

		groups.remove( anotherGroup );
		user.setGroups( groups );

		Assert.assertTrue( group.getUsers().size() == 1 );
		// This happens (and is expected) because there was no snapshot taken beforeQuery remove
		Assert.assertTrue( anotherGroup.getUsers().size() == 1 );
	}

	protected void cleanup() {
	}
}
