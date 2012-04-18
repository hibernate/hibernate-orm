package org.hibernate.envers.test.integration.jta;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Map;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestEntity;

import org.junit.Test;

import org.hibernate.testing.jta.TestingJtaBootstrap;

/**
 * Same as {@link org.hibernate.envers.test.integration.basic.Simple}, but in a JTA environment.
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaTransaction extends BaseEnversJPAFunctionalTestCase  {
    private Integer id1;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{IntTestEntity.class};
    }

    @Override
    protected void addConfigOptions(Map options) {
        TestingJtaBootstrap.prepare(options);
    }

    @Test
    @Priority(10)
    public void initData() throws Exception {
		TestingJtaBootstrap.INSTANCE.getTransactionManager().begin();

        EntityManager em;
        IntTestEntity ite;
        try {
            em = getEntityManager();
            ite = new IntTestEntity(10);
            em.persist(ite);
            id1 = ite.getId();
        } finally {
			TestingJtaBootstrap.tryCommit();
        }
        em.close();

		TestingJtaBootstrap.INSTANCE.getTransactionManager().begin();

        try {
            em = getEntityManager();
            ite = em.find(IntTestEntity.class, id1);
            ite.setNumber(20);
        } finally {
			TestingJtaBootstrap.tryCommit();
        }
        em.close();
    }

    @Test
    public void testRevisionsCounts() throws Exception {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(IntTestEntity.class, id1));
    }

    @Test
    public void testHistoryOfId1() {
        IntTestEntity ver1 = new IntTestEntity(10, id1);
        IntTestEntity ver2 = new IntTestEntity(20, id1);

        assert getAuditReader().find(IntTestEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(IntTestEntity.class, id1, 2).equals(ver2);
    }
}
