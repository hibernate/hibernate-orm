package org.hibernate.envers.test.integration.jta;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.EnversTestingJtaBootstrap;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestEntity;
import org.hibernate.testing.FailureExpected;

import org.junit.Test;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;
import java.util.Arrays;
import java.util.Properties;

import static org.hibernate.envers.test.EnversTestingJtaBootstrap.*;

/**
 * Same as {@link org.hibernate.envers.test.integration.basic.Simple}, but in a JTA environment.
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaTransaction extends AbstractEntityTest {
    private TransactionManager tm;
    private Integer id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @Override
    public void addConfigurationProperties(Properties configuration) {
        super.addConfigurationProperties(configuration);
        tm = EnversTestingJtaBootstrap.updateConfigAndCreateTM(configuration);
    }

    @Test
    @Priority(10)
    @FailureExpected(jiraKey = "HHH-6624")
    public void initData() throws Exception {
        tm.begin();

        EntityManager em;
        IntTestEntity ite;
        try {
            newEntityManager();
            em = getEntityManager();
            ite = new IntTestEntity(10);
            em.persist(ite);
            id1 = ite.getId();
        } finally {
            tryCommit(tm);
        }

        //

        tm.begin();

        try {
            newEntityManager();
            em = getEntityManager();
            ite = em.find(IntTestEntity.class, id1);
            ite.setNumber(20);
        } finally {
            tryCommit(tm);
        }
    }

    @Test
	@FailureExpected(jiraKey = "HHH-6624")
    public void testRevisionsCounts() throws Exception {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(IntTestEntity.class, id1));
    }

    @Test
	@FailureExpected(jiraKey = "HHH-6624")
    public void testHistoryOfId1() {
        IntTestEntity ver1 = new IntTestEntity(10, id1);
        IntTestEntity ver2 = new IntTestEntity(20, id1);

        assert getAuditReader().find(IntTestEntity.class, id1, 1).equals(ver1);
        assert getAuditReader().find(IntTestEntity.class, id1, 2).equals(ver2);
    }
}
