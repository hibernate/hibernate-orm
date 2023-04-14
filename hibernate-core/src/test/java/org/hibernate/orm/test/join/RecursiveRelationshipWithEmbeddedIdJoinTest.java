package org.hibernate.orm.test.join;

import jakarta.persistence.*;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@Jpa(annotatedClasses = {
        RecursiveRelationshipWithEmbeddedIdJoinTest.OrganizationWithPrimitiveId.class,
        RecursiveRelationshipWithEmbeddedIdJoinTest.OrganizationId.class,
        RecursiveRelationshipWithEmbeddedIdJoinTest.OrganizationWithEmbeddedId.class,
})
@TestForIssue(jiraKey = "HHH-15607")
public class RecursiveRelationshipWithEmbeddedIdJoinTest {

    public static final OrganizationId PARENT_EMBEDDED_ID = new OrganizationId(1);
    public static final OrganizationId CHILD_EMBEDDED_ID = new OrganizationId(2);
    public static final long PARENT_PRIMITIVE_ID = 1;
    public static final long CHILD_PRIMITIVE_ID = 2;

    @Test
    void findChildWithEmbeddedId(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    OrganizationWithEmbeddedId child = entityManager.createQuery(
                                    "SELECT child " +
                                            "  FROM OrganizationWithEmbeddedId child " +
                                            "  JOIN FETCH child.parent " +
                                            "  WHERE child.id = :id",
                                    OrganizationWithEmbeddedId.class)
                            .setParameter("id", CHILD_EMBEDDED_ID)
                            .getSingleResult();
                    assertThat(child, notNullValue());
                    assertThat(child.getName(), equalTo("Child organization"));
                    assertThat(child.getParent(), notNullValue());
                    assertThat(child.getParent().getName(), equalTo("Parent organization"));
                }
        );
    }

    @Test
    void findChildWithPrimitiveId(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    OrganizationWithPrimitiveId child = entityManager.createQuery(
                                    "SELECT child " +
                                            "  FROM OrganizationWithPrimitiveId child " +
                                            "  JOIN FETCH child.parent " +
                                            "  WHERE child.id = :id",
                                    OrganizationWithPrimitiveId.class)
                            .setParameter("id", CHILD_PRIMITIVE_ID)
                            .getSingleResult();
                    assertThat(child, notNullValue());
                    assertThat(child.getName(), equalTo("Child organization"));
                    assertThat(child.getParent(), notNullValue());
                    assertThat(child.getParent().getName(), equalTo("Parent organization"));
                }
        );
    }

    @BeforeEach
    void prepareTestData(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    final OrganizationWithEmbeddedId parent =
                            new OrganizationWithEmbeddedId(PARENT_EMBEDDED_ID,
                                    "Parent organization");
                    final OrganizationWithEmbeddedId child =
                            new OrganizationWithEmbeddedId(CHILD_EMBEDDED_ID,
                                    "Child organization");
                    entityManager.persist(parent);
                    entityManager.persist(child);
                    parent.addChild(child);
                }
        );
        scope.inTransaction(
                entityManager -> {
                    final OrganizationWithPrimitiveId parent =
                            new OrganizationWithPrimitiveId(PARENT_PRIMITIVE_ID,
                                    "Parent organization");
                    final OrganizationWithPrimitiveId child =
                            new OrganizationWithPrimitiveId(CHILD_PRIMITIVE_ID,
                                    "Child organization");
                    entityManager.persist(parent);
                    entityManager.persist(child);
                    parent.addChild(child);
                }
        );
    }

    @AfterEach
    void cleanUpTestData(EntityManagerFactoryScope scope) {
        scope.inTransaction(
                entityManager -> {
                    entityManager.createQuery(
                            "delete from OrganizationWithEmbeddedId where parent is not null").executeUpdate();
                    entityManager.createQuery(
                            "delete from OrganizationWithEmbeddedId where parent is null").executeUpdate();
                    entityManager.createQuery(
                            "delete from OrganizationWithPrimitiveId where parent is not null").executeUpdate();
                    entityManager.createQuery(
                            "delete from OrganizationWithPrimitiveId where parent is null").executeUpdate();
                }
        );
    }

    @Entity(name = "OrganizationWithEmbeddedId")
    public static class OrganizationWithEmbeddedId {

        @EmbeddedId
        private OrganizationId id;
        private String name;
        @ManyToOne
        private OrganizationWithEmbeddedId parent;
        @OneToMany(mappedBy = "parent")
        private Set<OrganizationWithEmbeddedId> children;

        public OrganizationWithEmbeddedId() {
        }

        public OrganizationWithEmbeddedId(OrganizationId id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public OrganizationWithEmbeddedId getParent() {
            return parent;
        }

        public void setParent(OrganizationWithEmbeddedId parent) {
            this.parent = parent;
        }

        public Set<OrganizationWithEmbeddedId> getChildren() {
            return children;
        }

        public void addChild(OrganizationWithEmbeddedId child) {
            if (children == null) {
                children = new HashSet<>();
            }
            children.add(child);
            child.setParent(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrganizationWithEmbeddedId that = (OrganizationWithEmbeddedId) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

    }

    @Embeddable
    public static class OrganizationId implements java.io.Serializable {

        private static final long serialVersionUID = 1L;
        @Column(name = "id")
        private long value;

        OrganizationId() {
            super();
        }

        OrganizationId(long value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrganizationId that = (OrganizationId) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
        
    }

    @Entity(name = "OrganizationWithPrimitiveId")
    public static class OrganizationWithPrimitiveId {

        @Id
        private long id;
        private String name;
        @ManyToOne
        private OrganizationWithPrimitiveId parent;
        @OneToMany(mappedBy = "parent")
        private Set<OrganizationWithPrimitiveId> children;

        public OrganizationWithPrimitiveId() {
        }

        public OrganizationWithPrimitiveId(long id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public OrganizationWithPrimitiveId getParent() {
            return parent;
        }

        public void setParent(OrganizationWithPrimitiveId parent) {
            this.parent = parent;
        }

        public Set<OrganizationWithPrimitiveId> getChildren() {
            return children;
        }

        public void addChild(OrganizationWithPrimitiveId child) {
            if (children == null) {
                children = new HashSet<>();
            }
            children.add(child);
            child.setParent(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrganizationWithPrimitiveId that = (OrganizationWithPrimitiveId) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

}
