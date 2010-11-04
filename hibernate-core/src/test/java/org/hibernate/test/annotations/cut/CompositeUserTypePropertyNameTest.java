package org.hibernate.test.annotations.cut;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.Work;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * @author Frode Carlsen
 */
public class CompositeUserTypePropertyNameTest extends TestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[] { Person.class };
    }

    @Override
    protected void configure(Configuration cfg) {
        super.configure(cfg);
        cfg.registerTypeOverride(AddressCompositeUserType.INSTANCE, new String[] { Address.class.getName() });
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        exportSchema(cfg, getSessions());
    }

    private static void exportSchema(final Configuration cfg, SessionFactory sessFact) {
        org.hibernate.classic.Session session = sessFact.openSession();
        session.doWork(new Work() {
            public void execute(final Connection conn) throws SQLException {
                SchemaExport schemaExport = new SchemaExport(cfg, conn);
                schemaExport.create(true, true);
            }
        });
        session.close();
    }

    public void testBasicOps() {
        Session session = openSession();
        session.beginTransaction();
        
        Person person = new Person("Steve", new Address());
        person.getAddress().setAddress1("123 Main");
        person.getAddress().setCity("Anywhere");
        
        session.persist(person);
        session.getTransaction().commit();
        session.close();

        session = openSession();
        session.beginTransaction();
        Person person1 = (Person) session.createQuery("from Person p where p.address.addr1 = '123 Main'").uniqueResult();
        assertTrue(person != person1);
        session.createQuery("from Person p where p.address.city = 'Anywhere'").list();
        person = (Person) session.load(Person.class, person.getId());
        session.delete(person);

        session.getTransaction().commit();
        session.close();
    }
}
