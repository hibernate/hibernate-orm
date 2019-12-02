/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.hhh13670;

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

@TestForIssue(jiraKey = "HHH-13670")
public class HHH13670Test extends BaseCoreFunctionalTestCase {

    @Before
    public void setUp() {
        doInJPA(this::sessionFactory, em -> {
            SubA a_1 = new SubA(1L);
            SubA a_2 = new SubA(2L);
            SubA a_3 = new SubA(3L);
            SubA a_14 = em.getReference(SubA.class, 10L);
            SubB b_4 = new SubB(4L, null);
            SubB b_5 = new SubB(5L, a_3);
            SubB b_6 = new SubB(6L, b_4);
            SubB b_7 = new SubB(7L, a_14);

            em.merge(a_1);
            em.merge(a_2);
            em.merge(a_3);
            em.merge(b_4);
            em.merge(b_5);
            em.merge(b_6);
            em.merge(b_7);
        });
    }

    @Test
    public void testDereferenceSuperClassAttributeInWithClause() {
        doInJPA(this::sessionFactory, em -> {
            em.createQuery("SELECT subB_0.id FROM SubB subB_0 LEFT JOIN subB_0.other subA_0 ON subA_0.id = subB_0.parent.id", Tuple.class).getResultList();
        });
    }

    @Test
    public void testRootTypeJoinWithGroupJoins() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery("SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 LEFT JOIN Super subA_0 ON subA_0.id = subB_0.parent.id ORDER BY subB_0.id ASC, subA_0.id ASC", Tuple.class)
                    .getResultList();

            assertEquals("Rows omitted despite optional association should have rendered a left join", 4, resultList.size());

            assertEquals((Long) 4L , resultList.get(0).get(0));
            assertEquals((Long) 5L , resultList.get(1).get(0));
            assertEquals((Long) 6L , resultList.get(2).get(0));
            assertEquals((Long) 7L , resultList.get(3).get(0));

            assertNull(resultList.get(0).get(1, Long.class));
            assertEquals((Long) 3L , resultList.get(1).get(1, Long.class));
            assertEquals((Long) 4L , resultList.get(2).get(1, Long.class));
            assertNull("Missing entry in foreign table should not be returned", resultList.get(3).get(1, Long.class));
        });
    }

    @Test
    public void testSubTypeJoinWithTableGroupJoins() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery("SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 LEFT JOIN SubA subA_0 ON subA_0.id = subB_0.parent.id ORDER BY subB_0.id ASC, subA_0.id ASC", Tuple.class)
                    .getResultList();

            assertEquals("Rows omitted despite optional association should have rendered a left join", 4, resultList.size());

            assertEquals((Long) 4L, resultList.get(0).get(0));
            assertEquals((Long) 5L, resultList.get(1).get(0));
            assertEquals((Long) 6L, resultList.get(2).get(0));
            assertEquals((Long) 7L, resultList.get(3).get(0));

            assertNull(resultList.get(0).get(1, Long.class));
            assertEquals((Long) 3L, resultList.get(1).get(1, Long.class));
            assertNull("Another subtype than queried for was returned", resultList.get(2).get(1));
            assertNull("Missing entry in foreign table should not be returned", resultList.get(3).get(1, Long.class));
        });
    }

    @Test
    public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery(
                    "SELECT  subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM subB_0.parent _synth_subquery_0 WHERE subA_0.id = _synth_subquery_0.id)) ORDER BY subB_0.id ASC, subA_0.id ASC", Tuple.class)
                    .getResultList();

            assertEquals(1, resultList.size());
        });
    }

    @Test
    public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery2() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery(
                    "SELECT  subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM Super s WHERE subA_0.id = s.parent.id)) ORDER BY subB_0.id ASC, subA_0.id ASC", Tuple.class)
                    .getResultList();

            assertEquals(4, resultList.size());
        });
    }

    @Test
    public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery3() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery(
                    "SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM Super s WHERE s.id = subB_0.parent.id)) ORDER BY subB_0.id ASC, subA_0.id ASC", Tuple.class)
                    .getResultList();

            assertEquals(6, resultList.size());
        });
    }

    @Test
    public void testSubTypePropertyReferencedFromEntityJoinInSyntheticSubquery4() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery(
                    "SELECT subB_0.id, subA_0.id, subB_0.id, subA_0.id FROM SubB subB_0 INNER JOIN SubA subA_0 ON 1=1 WHERE (EXISTS (SELECT 1 FROM Super s WHERE s.id = subA_0.parent.id)) ORDER BY subB_0.id ASC, subA_0.id ASC", Tuple.class)
                    .getResultList();

            assertEquals(0, resultList.size());
        });
    }

    @Test
    public void testSubTypePropertyReferencedFromWhereClause() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery("SELECT subB_0.id FROM SubB subB_0 WHERE subB_0.parent.id IS NOT NULL", Tuple.class)
                    .getResultList();
        });
    }

    @Test
    public void testSubTypePropertyReferencedFromGroupByClause() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery("SELECT subB_0.id FROM SubB subB_0 GROUP BY subB_0.id , subB_0.parent.id", Tuple.class)
                    .getResultList();
        });
    }

    @Test
    public void testSubTypePropertyReferencedFromOrderByClause() {
        doInJPA(this::sessionFactory, em -> {
            List<Tuple> resultList = em.createQuery("SELECT subB_0.id FROM SubB subB_0 ORDER BY subB_0.id , subB_0.parent.id", Tuple.class)
                    .getResultList();
        });
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Super.class, SubA.class, SubB.class };
    }

    @Entity(name = "Super")
    @Inheritance(strategy = InheritanceType.JOINED)
    public static class Super<SubType extends Super> {

        @Id
        @Column
        Long id;

        @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        @ManyToOne(targetEntity = Super.class, fetch = FetchType.LAZY)
        SubType parent;

    }

    @Entity(name = "SubA")
    public static class SubA extends Super {

        SubA() {}

        SubA(Long id) {
            this.id = id;
        }

    }

    @Entity(name = "SubB")
    public static class SubB extends Super<SubA> {

        @ManyToOne(fetch = FetchType.LAZY)
        Super other;

        SubB() {}

        SubB(Long id, Super parent) {
            this.id = id;
            ((Super) this).parent = parent;
        }

    }

}
