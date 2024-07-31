package org.hibernate.orm.test.cut.generic;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;


@JiraKey(value = "HHH-17019")
public class GenericCompositeUserTypeTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                GenericCompositeUserTypeEntity.class
        };
    }

    @Test
    public void hhh17019Test() throws Exception {
        Session s = openSession();
        Transaction tx = s.beginTransaction();

        EnumPlaceholder<Weekdays, Weekdays> placeholder = new EnumPlaceholder<>( Weekdays.MONDAY, Weekdays.SUNDAY );
        GenericCompositeUserTypeEntity entity = new GenericCompositeUserTypeEntity( placeholder );

        s.persist( entity );

        tx.commit();
        s.close();
    }
}
