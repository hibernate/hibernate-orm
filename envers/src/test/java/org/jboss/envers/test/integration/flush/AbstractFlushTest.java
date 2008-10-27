package org.jboss.envers.test.integration.flush;

import org.jboss.envers.test.AbstractEntityTest;
import org.jboss.envers.test.entities.StrTestEntity;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.ejb.Ejb3Configuration;
import org.testng.annotations.BeforeClass;

import javax.persistence.EntityManager;
import java.io.IOException;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public abstract class AbstractFlushTest extends AbstractEntityTest {
    public abstract FlushMode getFlushMode();

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    private static Session getSession(EntityManager em) {
        Object delegate = em.getDelegate();
        if (delegate instanceof Session) {
            return (Session) delegate;
        } else if (delegate instanceof EntityManager) {
            Object delegate2 = ((EntityManager) delegate).getDelegate();

            if (delegate2 instanceof Session) {
                return (Session) delegate2;
            }
        }

        throw new RuntimeException("Invalid entity manager");
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initFlush() throws IOException {
        Session session = getSession(getEntityManager());
        session.setFlushMode(getFlushMode());
    }
}
