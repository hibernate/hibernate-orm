package org.hibernate.orm.test.annotations.refcolnames.misc;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Where;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = {MiscTest.EntityA.class, MiscTest.EntityB.class, MiscTest.A.class, MiscTest.B.class})
@SessionFactory
public class MiscTest {
    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction(s->{
            EntityA a = new EntityA();
            EntityB b = new EntityB();
            a.entityB = b;
            b.entityA = a;
            s.persist(a);
            s.persist(b);
        });
        scope.inTransaction(s-> {
            String aIdOne = "boo";

            A a2 = new A(aIdOne, "baa");
            B b = new B("some2", "thing2");
            s.persist(a2);
            a2.b_R1 = b;
            b.a_R1 = a2;
            s.persist(b);

            A result = s.createQuery("SELECT a FROM A a WHERE a.id.aOne = :param", A.class)
                    .setParameter("param", aIdOne).getSingleResult();
            assertEquals( result.id.aOne, aIdOne );
        });
    }

    @Entity
    public static class EntityA implements Serializable {
        @Id long entityBKey;
        @Id boolean flag;
        @OneToOne(mappedBy = "entityA")
        public EntityB entityB;
    }

    @Entity
    public static class EntityB implements Serializable {
        @Id long entityBKey;
        @Id boolean flag;
        @OneToOne
        @JoinColumn(name = "entityBKey", referencedColumnName = "entityBKey", insertable = false, updatable = false)
        @JoinColumn(name = "flag", referencedColumnName = "flag", insertable = false, updatable = false)
        @Where(clause = "flag=1")
        public EntityA entityA;
    }

    @Entity(name = "A")
    @Table(name = "a_table")
    public static class A {

        @EmbeddedId
        private AId id;

        // primary side of relationship
        @OneToOne(mappedBy = "a_R1", fetch = FetchType.LAZY, targetEntity = B.class)
        private B b_R1;

        public A(String aOne, String aTwo) {
            this.id = new AId(aOne, aTwo);
        }

    }

    @Embeddable
    public static class AId implements Serializable {

        @Column(name = "a_one", nullable = false, length = 4096)
        private String aOne;

        @Column(name = "a_two", nullable = false, length = 4096)
        private String aTwo;

        private AId() {}

        public AId(String aOne, String aTwo) {
            this.aOne = aOne;
            this.aTwo = aTwo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(aOne, aTwo);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            AId other = (AId) obj;
            return Objects.equals(aOne, other.aOne) && Objects.equals(aTwo, other.aTwo);
        }
    }

    @Entity(name = "B")
    @Table(name = "b_table")
    public static class B {

        @EmbeddedId
        private BId id;
        // secondary side of relationship
        @OneToOne(targetEntity = A.class, fetch = FetchType.LAZY)
        @JoinColumns(value = {
                @JoinColumn(name = "b_a_two", referencedColumnName = "a_two", nullable = true, insertable = true,
                        updatable = true),
                @JoinColumn(name = "b_a_one", referencedColumnName = "a_one", nullable = true, insertable = true,
                        updatable = true)})
        private A a_R1;

        public B(String bOne, String bTwo) {
            this.id = new BId(bOne, bTwo);
        }

    }

    @Embeddable
    public static class BId implements Serializable {

        @Column(name = "b_one", nullable = false, length = 4096)
        private String bOne = new String("");

        @Column(name = "b_two", nullable = false, length = 4096)
        private String bTwo = new String("");

        private BId() {}

        public BId(String bOne, String bTwo) {
            this.bOne = bOne;
            this.bTwo = bTwo;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bOne, bTwo);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            BId other = (BId) obj;
            return Objects.equals(bOne, other.bOne) && Objects.equals(bTwo, other.bTwo);
        }

    }
}
