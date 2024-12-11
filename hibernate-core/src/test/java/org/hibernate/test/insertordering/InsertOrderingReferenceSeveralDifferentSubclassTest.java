package org.hibernate.test.insertordering;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Normunds Gavars
 */
@TestForIssue( jiraKey = "HHH-14344" )
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingReferenceSeveralDifferentSubclassTest extends BaseInsertOrderingTest {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                BaseEntity.class,
                SubclassZero.class,
                SubclassOne.class,
                SubclassTwo.class,
                UnrelatedEntity.class
        };
    }

    @Test
    public void testSubclassReferenceChain() {
        doInHibernate(this::sessionFactory, session -> {
            UnrelatedEntity unrelatedEntity1 = new UnrelatedEntity();
            session.save(unrelatedEntity1);

            SubclassZero subclassZero = new SubclassZero();
            session.save(subclassZero);
            SubclassOne subclassOne = new SubclassOne();
            subclassOne.setParent(subclassZero);
            SubclassTwo subclassTwo = new SubclassTwo();
            subclassTwo.setParent(subclassOne);

            session.save(subclassTwo);
            session.save(subclassOne);

            // add extra instances for the sake of volume
            UnrelatedEntity unrelatedEntity2 = new UnrelatedEntity();
            session.save(unrelatedEntity2);
            SubclassOne subclassOne2 = new SubclassOne();
            SubclassTwo subclassD2 = new SubclassTwo();
            session.save(subclassOne2);
            session.save(subclassD2);

            clearBatches();
        });

        verifyContainsBatches(
                new Batch( "insert into UnrelatedEntity (unrelatedValue, id) values (?, ?)", 2 ),
                new Batch( "insert into BaseEntity (TYPE, id) values ('ZERO', ?)"),
                new Batch( "insert into BaseEntity (PARENT_ID, TYPE, id) values (?, 'TWO', ?)", 2 ),
                new Batch( "insert into BaseEntity (PARENT_ID, TYPE, id) values (?, 'ONE', ?)", 2 )
        );
    }

    @Entity(name = "BaseEntity")
    @DiscriminatorColumn(name = "TYPE")
    @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
    public static abstract class BaseEntity {

        @Id
        @GeneratedValue
        private Long id;
    }

    @Entity(name = "SubclassZero")
    @DiscriminatorValue("ZERO")
    public static class SubclassZero extends BaseEntity {

    }

    @Entity(name = "SubclassOne")
    @DiscriminatorValue("ONE")
    public static class SubclassOne extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "PARENT_ID")
        private SubclassZero parent;

        @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST, orphanRemoval = true, mappedBy = "parent", targetEntity = SubclassTwo.class)
        private List<SubclassTwo> subclassTwoList = new ArrayList<>();

        public void setParent(SubclassZero parent) {
            this.parent = parent;
        }
    }

    @Entity(name ="SubclassTwo")
    @DiscriminatorValue("TWO")
    public static class SubclassTwo extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "PARENT_ID")
        private SubclassOne parent;

        public void setParent(SubclassOne parent) {
            this.parent = parent;
        }
    }

    @Entity(name = "UnrelatedEntity")
    public static class UnrelatedEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String unrelatedValue;

        public void setUnrelatedValue(String unrelatedValue) {
            this.unrelatedValue = unrelatedValue;
        }
    }
}
