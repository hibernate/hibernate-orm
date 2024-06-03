/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Luis Barreiro
 */
@BytecodeEnhanced
public class ManyToManyAssociationListTest {
    @Test
    public void testBidirectionalExisting() {
        Group group = new Group();
        Group anotherGroup = new Group();

        User user = new User();
        anotherGroup.users.add( user );

        user.setGroups( new ArrayList<>( Collections.singleton( group ) ) );
        user.setGroups( new ArrayList<>( Arrays.asList( group, anotherGroup ) ) );

        assertEquals( 1, group.getUsers().size() );
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
        List<User> users = new ArrayList<>();

        List<User> getUsers() {
            return Collections.unmodifiableList( users );
        }

        void resetUsers() {
            // this wouldn't trigger association management: users.clear();
            users = new ArrayList<>();
        }
    }

    @Entity
    private static class User {

        @Id
        Long id;

        String password;

        @ManyToMany
        List<Group> groups;

        void addGroup(Group group) {
            List<Group> groups = this.groups == null ? new ArrayList<>() : this.groups;
            groups.add( group );
            this.groups = groups;
        }

        List<Group> getGroups() {
            return Collections.unmodifiableList( groups );
        }

        void setGroups(List<Group> groups) {
            this.groups = groups;
        }
    }
}
