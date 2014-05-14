package org.hibernate.test.id;

import static org.junit.Assert.assertTrue;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class SequenceGeneratorWithInitialValueTest extends BaseCoreFunctionalTestCase {

    @Override
    public String[] getMappings() {
        return new String[] { "id/PersonWithSequenceInitialValue.hbm.xml" };
    }

    /**
     * SQL server requires that sequence be initialized to something other than the minimum value for the type
	 * (e.g., Long.MIN_VALUE). For generator = "sequence", the initial value must be provided as a parameter.
	 * For this test, the sequence is initialized to 10.
	 */
    @Test
    @TestForIssue(jiraKey = "HHH-8814")
	@FailureExpectedWithNewMetamodel(jiraKey = "HHH-9189")
    @RequiresDialect(value={SQLServer2012Dialect.class, H2Dialect.class})
    public void testStartOfSequence() throws Exception {
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        final Person person = new Person();
        s.persist(person);
        tx.commit();
        s.close();

        assertTrue(person.getId() == 10);

		s = openSession();
		tx = s.beginTransaction();
		s.createQuery( "delete from Person" ).executeUpdate();
		tx.commit();
		s.close();

	}

}
