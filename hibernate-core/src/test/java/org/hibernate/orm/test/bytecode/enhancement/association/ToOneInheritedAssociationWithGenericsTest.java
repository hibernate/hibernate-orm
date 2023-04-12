/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

@TestForIssue( jiraKey = "HHH-16459" )
@RunWith( BytecodeEnhancerRunner.class )
@EnhancementOptions(inlineDirtyChecking = true)
public class ToOneInheritedAssociationWithGenericsTest {

    @Test
    public void test() {
        var entity = new ChildItem();
        EnhancerTestUtils.checkDirtyTracking( entity );

        entity.setOther( new Other() );
        EnhancerTestUtils.checkDirtyTracking( entity, "other" );
    }

    @Entity
    public static class Other {

        @Id
        @GeneratedValue
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    @MappedSuperclass
    public static abstract class Item<T> {
        @ManyToOne( fetch = FetchType.LAZY )
        private T other;

        public T getOther() {
            return other;
        }

        public void setOther(T other) {
            this.other = other;
        }
    }

    @Entity
    public static class ChildItem extends Item<Other> {
        @Id
        @GeneratedValue
        private Long id;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
