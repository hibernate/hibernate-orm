package org.hibernate.test.annotations.uniqueconstraint;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Manuel Bernhardt <bernhardt.manuel@gmail.com>
 */
public class UniqueConstraintInheritanceTest extends TestCase {


    public void testUniquenessConstraintInMappedSuperclass() throws Exception {
        Session s = openSession();
        Transaction tx = s.beginTransaction();

        Matter m = new Matter();
        m.setId(1l);
        m.setWeight(100);
        s.persist(m);
        s.flush();

        Subspace s1 = new Subspace();
        s1.setId(1l);
        s1.setMatter(m);
        s1.setValue(0);
        s.persist(s1);
        s.flush();

        Subspace s2 = new Subspace();
        s2.setId(2l);
        s2.setMatter(m);
        s2.setValue(0);
        s.persist(s2);
        try {
            s.flush();
            fail("Unique constraint not applied");
        } catch (JDBCException e) {
            //success
        }

        tx.rollback();
        s.close();
    }

    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                Space.class,
                Subspace.class,
                Matter.class
        };
    }

}
