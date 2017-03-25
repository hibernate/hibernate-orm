package org.hibernate.test.bytecode.enhancement.detached;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Luis Barreiro
 */
public class DetachedGetIdentifierTestTask extends AbstractEnhancerTestTask {

    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{SimpleEntity.class};
    }

    public void prepare() {
        Configuration cfg = new Configuration();
        cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
        cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
        super.prepare( cfg );
    }

    public void execute() {
        EntityManager em = getFactory().createEntityManager();
        em.getTransaction().begin();

        SimpleEntity se = new SimpleEntity();
        se.name = "test";
        se = em.merge( se );

        Assert.assertNotNull( getFactory().getPersistenceUnitUtil().getIdentifier( se ) );

        em.getTransaction().commit();
        em.close();

        // Call as detached entity
        Assert.assertNotNull( getFactory().getPersistenceUnitUtil().getIdentifier( se ) );
    }

    protected void cleanup() {
    }

    @Entity(name = "SimpleEntity")
    public static class SimpleEntity {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

    }
}
