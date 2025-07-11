/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.tools.TestTools;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-16251")
public class EntityWithChangesInOneToManyRelationsQueryTest extends BaseEnversJPAFunctionalTestCase {
    private Integer parentId;
    private Integer child1Id;
    private Integer child2Id;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{Parent.class, Child1.class, Child2.class};
    }

    @Test
    @Priority(10)
    public void initData() {
        // Revision 1
        parentId = doInJPA(this::entityManagerFactory, entityManager -> {
            final Parent parent = new Parent();
            parent.setValue(25);
            entityManager.persist(parent);
            return parent.getId();
        });

        // Revision 2
        doInJPA(this::entityManagerFactory, entityManager -> {
            final Parent parent = entityManager.find(Parent.class, parentId);
            parent.setValue(30);

            Child1 child1 = new Child1();
            child1.setValue(5);
            child1.setParent(parent);
            parent.setChild1s(TestTools.makeSet(child1));
            entityManager.persist(child1);
            child1Id = child1.id;

            Child2 child2 = new Child2();
            child2.setValue(10);
            child2.setParent(parent);
            parent.setChild2s(TestTools.makeSet(child2));
            entityManager.persist(child2);
            child2Id = child2.id;

            entityManager.merge(parent);
        });

        // Revision 3
        doInJPA(this::entityManagerFactory, entityManager -> {
            final Parent parent = entityManager.find(Parent.class, parentId);
            parent.setValue(40);

            final Child1 child1 = entityManager.find(Child1.class, child1Id);
            entityManager.remove(child1);
            parent.setChild1s(new HashSet<>());

            final Child2 child2 = entityManager.find(Child2.class, child2Id);
            entityManager.remove(child2);
            parent.setChild2s(new HashSet<>());

            entityManager.merge(parent);
        });

        // Revision 4
        doInJPA(this::entityManagerFactory, entityManager -> {
            final Parent parent = entityManager.find(Parent.class, parentId);

            Child1 child1 = new Child1();
            child1.setValue(5);
            child1.setParent(parent);
            parent.setChild1s(TestTools.makeSet(child1));
            entityManager.persist(child1);
            child1Id = child1.id;

            Child2 child2 = new Child2();
            child2.setValue(10);
            child2.setParent(parent);
            parent.setChild2s(TestTools.makeSet(child2));
            entityManager.persist(child2);
            child2Id = child2.id;

            entityManager.merge(parent);
        });
    }

    @Test
    public void testRevisionCount() {
        assertEquals(Arrays.asList(1, 2, 3, 4), getAuditReader().getRevisions(Parent.class, parentId));
    }

    @Test
    public void testEntityRevisionsWithChangesQueryNoDeletions() {
        List results = getAuditReader().createQuery()
                .forRevisionsOfEntityWithChanges(Parent.class, false)
                .add(AuditEntity.id().eq(parentId))
                .getResultList();
        compareResults(getExpectedResults(), results);
    }

    private void compareResults(List<Object[]> expectedResults, List results) {
        assertEquals(expectedResults.size(), results.size());
        for (int i = 0; i < results.size(); ++i) {
            final Object[] row = (Object[]) results.get(i);
            final Object[] expectedRow = expectedResults.get(i);
            // the query returns 4, index 1 has the revision entity which we don't test here
            assertEquals(4, row.length);
            // because we don't test the revision entity, we adjust indexes between the two arrays
            assertEquals(expectedRow[0], row[0]);
            assertEquals(expectedRow[1], row[2]);
            assertEquals(expectedRow[2], row[3]);
        }
    }

    protected List<Object[]> getExpectedResults() {

        final List<Object[]> results = new ArrayList<>();

        results.add(
                new Object[]{
                        new Parent(parentId, 25),
                        RevisionType.ADD,
                        Collections.emptySet()
                }
        );

        results.add(
                new Object[]{
                        new Parent(parentId, 30),
                        RevisionType.MOD,
                        TestTools.makeSet("value", "child1s", "child2s")
                }
        );

        results.add(
                new Object[]{
                        new Parent(parentId, 40),
                        RevisionType.MOD,
                        TestTools.makeSet("value", "child1s", "child2s")
                }
        );

        results.add(
                new Object[]{
                        new Parent(parentId, 40),
                        RevisionType.MOD,
                        TestTools.makeSet("child1s", "child2s")
                }
        );

        System.out.println("Generated " + results.size() + " results.");
        return results;
    }

    @Audited
    @Entity(name = "Parent")
    public static class Parent {
        @Id
        @GeneratedValue
        private Integer id;

        @Column(name = "val")
        @Audited(withModifiedFlag = true, modifiedColumnName = "value_MOD")
        private Integer value;

        @OneToMany(mappedBy = "parent")
        @Audited(withModifiedFlag = true, modifiedColumnName = "child1s_MOD")
        private Set<Child1> child1s;

        @OneToMany(mappedBy = "parent")
        @Audited(withModifiedFlag = true, modifiedColumnName = "child2s_MOD")
        private Set<Child2> child2s;

        Parent() {

        }

        Parent(Integer id, Integer value) {
            this.id = id;
            this.value = value;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Set<Child1> getChild1s() {
            return child1s;
        }

        public void setChild1s(Set<Child1> child1s) {
            this.child1s = child1s;
        }

        public Set<Child2> getChild2s() {
            return child2s;
        }

        public void setChild2s(Set<Child2> child2s) {
            this.child2s = child2s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Parent parent = (Parent) o;
            return Objects.equals(id, parent.id) && Objects.equals(value, parent.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "Parent{" +
                    "id=" + id +
                    ", value=" + value +
                    '}';
        }
    }

    @Audited
    @Entity(name = "Child1")
    public static class Child1 {
        @Id
        @GeneratedValue
        private Integer id;

        @Column(name = "val")
        @Audited(withModifiedFlag = true, modifiedColumnName = "value_MOD")
        private Integer value;

        @ManyToOne
        @Audited(withModifiedFlag = true, modifiedColumnName = "parent_MOD")
        private Parent parent;

        Child1() {

        }

        Child1(Integer id, Integer value) {
            this.id = id;
            this.value = value;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Child1 child1 = (Child1) o;
            return Objects.equals(id, child1.id) && Objects.equals(value, child1.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "Child1{" +
                    "id=" + id +
                    ", value=" + value +
                    '}';
        }
    }

    @Audited
    @Entity(name = "Child2")
    public static class Child2 {
        @Id
        @GeneratedValue
        private Integer id;

        @Column(name = "val")
        @Audited(withModifiedFlag = true, modifiedColumnName = "value_MOD")
        private Integer value;

        @ManyToOne
        @Audited(withModifiedFlag = true, modifiedColumnName = "parent_MOD")
        private Parent parent;

        Child2() {

        }

        Child2(Integer id, Integer value) {
            this.id = id;
            this.value = value;
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Parent getParent() {
            return parent;
        }

        public void setParent(Parent parent) {
            this.parent = parent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Child2 child2 = (Child2) o;
            return Objects.equals(id, child2.id) && Objects.equals(value, child2.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, value);
        }

        @Override
        public String toString() {
            return "Child2{" +
                    "id=" + id +
                    ", value=" + value +
                    '}';
        }
    }
}
