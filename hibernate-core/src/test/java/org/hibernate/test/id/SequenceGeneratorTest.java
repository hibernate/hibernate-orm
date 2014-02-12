package org.hibernate.test.id;

import static org.junit.Assert.assertTrue;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class SequenceGeneratorTest extends BaseCoreFunctionalTestCase {

    @Override
    public String[] getMappings() {
        return new String[] { "id/Person.hbm.xml" };
    }

    /**
     * This seems a little trivial, but we need to guarantee that all Dialects start their sequences on a non-0 value.
     */
    @Test
    @TestForIssue(jiraKey = "HHH-8814")
    @RequiresDialectFeature(DialectChecks.SupportsSequences.class)
    public void testStartOfSequence() throws Exception {
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        final Person person = new Person();
        s.persist(person);
        tx.commit();
        s.close();
        
        assertTrue(person.getId() > 0);
    }

}
