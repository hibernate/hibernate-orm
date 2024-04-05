/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-9529" )
@DomainModel(
        annotatedClasses = {
              CrossEnhancementTest.Parent.class, CrossEnhancementTest.Child.class, CrossEnhancementTest.ChildKey.class
        }
)
@SessionFactory
@BytecodeEnhanced
public class CrossEnhancementTest {

    @Test
    public void test(SessionFactoryScope scope) {
		//        sessionFactory().close();
		//        buildSessionFactory();
		scope.getSessionFactory().close();
        // TODO: I do not get this test ^ and not sure how to update it ...
	}

    // --- //

    @Entity
    @Table( name = "PARENT" )
    static class Parent {
        @Id
        String id;
    }

    @Embeddable
    static class ChildKey implements Serializable {
        String parent;
        String type;
    }

    @Entity
    @Table( name = "CHILD" )
    static class Child {
        @EmbeddedId
        ChildKey id;

        @MapsId( "parent" )
        @ManyToOne
        Parent parent;

        public String getfieldOnChildKeyParent() {
            // Note that there are two GETFIELD ops here, one on the field 'id' that should be enhanced and another
            // on the field 'parent' that may be or not (depending if 'extended enhancement' is enabled)

            // Either way, the field 'parent' on ChildKey should not be confused with the field 'parent' on Child

            return id.parent;
        }
    }
}
