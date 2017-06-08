/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone.hh11746;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Jonathan Ihm
 */
public class OneToOneCascadeMapsId extends BaseCoreFunctionalTestCase {
    @Test
    public void hhh11746Test() throws Exception {
        // BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
        Session s = openSession();
        Transaction tx = s.beginTransaction();
        insertTestData(s);
        tx.commit();

        s.clear();
        tx = s.beginTransaction();
        Person person = s.find(Person.class, new PersonId("a1", "s1"));
        Address adr = person.getAddress();
        assertEquals(person.getId(), adr.getId());

        person = s.find(Person.class, new PersonId("a2", "s2"));
        adr = person.getAddress();
        assertEquals(person.getId(), adr.getId());

        tx.commit();
        s.close();
    }



    private void insertTestData(Session s) {
        Person person1 = new Person();
        person1.setId(new PersonId("p1", "s1"));
        Address address1 = new Address();
        address1.street = "address1";
        person1.setAddress(address1);
        s.persist(person1);

        Person person2 = new Person();
        person2.setId(new PersonId("p2", "s2"));
        Address address2 = new Address();
        address2.street = "address2";
        person2.setAddress(address2);
        s.persist(person2);
    }

    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[]{
                Address.class,
                Person.class,
        };
    }
}
