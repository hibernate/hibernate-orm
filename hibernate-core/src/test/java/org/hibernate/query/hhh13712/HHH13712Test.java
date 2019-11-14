/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh13712;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Tuple;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@TestForIssue(jiraKey = "HHH-13712")
public class HHH13712Test extends BaseCoreFunctionalTestCase {

    @Before
    public void setUp() {
        doInJPA(this::sessionFactory, em -> {
            SomeOther a_1 = new SomeOther(1L);
            SomeOther a_2 = new SomeOther(2L);
            SomeOther a_3 = new SomeOther(3L);
            SubObject b_5 = new SubObject(5L, a_1);
            SubObject b_6 = new SubObject(6L, a_2);
            SubObject b_7 = new SubObject(7L, a_3);

            em.merge(a_1);
            em.merge(a_2);
            em.merge(a_3);
            em.merge(b_5);
            em.merge(b_6);
            em.merge(b_7);
        });
    }

    @Test
    public void testJoinSuperclassAssociationOnly() {
        doInJPA(this::sessionFactory, em -> {
            List<Integer> actual = em.createQuery("SELECT 1 FROM SubObject sub LEFT JOIN sub.parent p", Integer.class).getResultList();
            assertEquals(3, actual.size());
        });
    }

    @Test
    public void testJoinSuperclassAssociation() {
        doInJPA(this::sessionFactory, em -> {
            long actual = em.createQuery("SELECT COUNT(sub) FROM SubObject sub LEFT JOIN sub.parent p WHERE p.id = 1", Long.class).getSingleResult();
            assertEquals(1L, actual);
        });
    }

    @Test
    public void testCountParentIds() {
        doInJPA(this::sessionFactory, em -> {
            long actual = em.createQuery("SELECT COUNT(distinct sub.parent.id) FROM SubObject sub", Long.class).getSingleResult();
            assertEquals(3L, actual);
        });
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Super.class, SubObject.class, SomeOther.class };
    }

    @Entity(name = "Super")
    @Inheritance(strategy = InheritanceType.JOINED)
    public static class Super {

        @Id
        @Column
        Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false)
        SomeOther parent;

    }

    @Entity(name = "SubObject")
    public static class SubObject extends Super {

        SubObject() {}

        SubObject(Long id, SomeOther parent) {
            this.id = id;
            this.parent = parent;
        }

    }

    @Entity(name = "SomeOther")
    public static class SomeOther {

        @Id
        @Column
        Long id;

        SomeOther() {}

        SomeOther(Long id) {
            this.id = id;
        }
    }

}
