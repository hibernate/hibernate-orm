package org.hibernate.test.id;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class SequenceGeneratorTest extends BaseCoreFunctionalTestCase {

    @Override
    public String[] getMappings() {
        return new String[] { "id/Person.hbm.xml" };
    }

    @Test
    public void testDistinctId() throws Exception {
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        final int testLength = 8;
        final Person[] persons = new Person[testLength];
        for (int i = 0; i < testLength; i++) {
            persons[i] = new Person();
            s.persist(persons[i]);
        }
        tx.commit();
        s.close();
        for (int i = 0; i < testLength; i++) {
            assertEquals(i + 1, persons[i].getId().intValue());
        }

        s = openSession();
        tx = s.beginTransaction();
        s.createQuery("delete from Person").executeUpdate();
        tx.commit();
        s.close();
    }

}
