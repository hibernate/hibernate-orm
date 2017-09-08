package org.hibernate.event;

import javax.persistence.*;

import org.hibernate.Session;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;


/**
 * @author Chris Cranford
 */
@TestForIssue( jiraKey = "HHH-11721" )
public class EventEvictionTest extends BaseCoreFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { A.class, B.class };
    }

    @Override
    protected void afterSessionFactoryBuilt() {
        super.afterSessionFactoryBuilt();
        EventListenerRegistry registry = sessionFactory().getServiceRegistry().getService( EventListenerRegistry.class );
        registry.appendListeners(
                EventType.PRE_INSERT,
                new PreInsertEventListener() {
                    @Override
                    public boolean onPreInsert(PreInsertEvent event) {
                        if(event.getEntity() instanceof B) {
                            return true;
                        }
                        return false;
                    }
                }
        );
    }

    @Test
    public void testInsertVeto() throws Exception {
        Session session = openSession();
        session.beginTransaction();

        A a = new A();
        B b = new B();
        b.setField1( "f1" );
        b.setfield2( "f2" );
        a.setB( b );
        session.save( a );
        session.flush();

        session.getTransaction().commit();
        session.close();
    }

    @Entity(name = "A")
    public static class A{
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @OneToOne(cascade = CascadeType.ALL)
        private B b;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public B getB() {
            return b;
        }

        public void setB(B b) {
            this.b = b;
        }
    }

    @Entity(name = "B")
    public static class B {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;
        private String field1;
        private String field2;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public String getField2() {
            return field2;
        }

        public void setfield2(String field2) {
            this.field2 = field2;
        }
    }
}
