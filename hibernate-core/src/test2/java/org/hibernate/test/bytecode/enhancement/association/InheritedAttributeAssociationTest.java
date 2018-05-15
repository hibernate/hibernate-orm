/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import java.util.List;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-11050" )
@RunWith( BytecodeEnhancerRunner.class )
public class InheritedAttributeAssociationTest {

    @Test
    public void test() {
        // The mapping is wrong but the point is that the enhancement phase does not need to fail. See JIRA for further detail

        // If enhancement of 'items' attribute fails, 'name' won't be enhanced
        Author author = new Author();
        author.name = "Bernardo Soares";
        EnhancerTestUtils.checkDirtyTracking( author, "name" );
    }

    // --- //

    @Entity
    private static class Author {

        @Id
        @GeneratedValue
        Long id;

        @OneToMany( fetch = FetchType.LAZY, mappedBy = "author" )
        List<ChildItem> items;

        // keep this field after 'items'
        String name;
    }

    @MappedSuperclass
    @Inheritance( strategy = InheritanceType.SINGLE_TABLE )
    @DiscriminatorColumn( name = "type", discriminatorType = DiscriminatorType.STRING )
    private static abstract class Item {

        @Id
        @GeneratedValue
        Long id;

        @ManyToOne( fetch = FetchType.LAZY )
        Author author;
    }

    @Entity
    @DiscriminatorValue( "child" )
    private static class ChildItem extends Item {
    }
}
