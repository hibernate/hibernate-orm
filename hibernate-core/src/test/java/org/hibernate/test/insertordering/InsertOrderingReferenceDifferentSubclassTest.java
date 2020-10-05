package org.hibernate.test.insertordering;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Normunds Gavars
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14227" )
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class InsertOrderingReferenceDifferentSubclassTest extends BaseInsertOrderingTest {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                SubclassA.class,
                SubclassB.class
        };
    }

    @Test
    public void testReferenceDifferentSubclass() {
        doInHibernate(this::sessionFactory, session -> {
            SubclassA subclassA1 = new SubclassA();
            SubclassB subclassB1 = new SubclassB();

            SubclassA subclassA2 = new SubclassA();
            SubclassB subclassB2 = new SubclassB();

            subclassA1.referenceB = subclassB2;
            subclassB2.referenceA = subclassA2;

            subclassA2.referenceB = subclassB1;

            session.save( subclassA1 );
            session.save( subclassA2 );

            clearBatches();
        });

        verifyContainsBatches(
                new Batch( "insert into SubclassB (referenceA_id, id) values (?, ?)", 2 ),
                new Batch( "insert into SubclassA (referenceB_id, id) values (?, ?)", 2 )
        );
    }

    @MappedSuperclass
    static class BaseClass {

        @Id @GeneratedValue
        Long id;

    }

    @Entity(name = "SubclassA")
    static class SubclassA extends BaseClass {

        @OneToOne(cascade = CascadeType.ALL)
        SubclassB referenceB;

    }

    @Entity(name = "SubclassB")
    static class SubclassB extends BaseClass {

        @ManyToOne(fetch = FetchType.LAZY)
        SubclassA referenceA;

    }
}
