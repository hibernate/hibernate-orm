/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
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

        Assert.assertEquals( 2, group.getUsers().size() );
        Assert.assertEquals( 1, anotherGroup.getUsers().size() );

        group.resetUsers();

        Assert.assertEquals( 1, user.getGroups().size() );
        Assert.assertEquals( 0, anotherUser.getGroups().size() );

        // Test remove
        user.addGroup( group );
        anotherUser.addGroup( group );

        Assert.assertEquals( 2, group.getUsers().size() );
        Assert.assertEquals( 1, anotherGroup.getUsers().size() );

        Set<Group> groups = new HashSet<>( user.getGroups() );
        groups.remove( group );
        user.setGroups( groups );

        Assert.assertEquals( 1, group.getUsers().size() );
        Assert.assertEquals( 1, anotherGroup.getUsers().size() );

        groups.remove( anotherGroup );
        user.setGroups( groups );

        Assert.assertEquals( 1, group.getUsers().size() );
        // This happens (and is expected) because there was no snapshot taken before remove
        Assert.assertEquals( 1, anotherGroup.getUsers().size() );
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
