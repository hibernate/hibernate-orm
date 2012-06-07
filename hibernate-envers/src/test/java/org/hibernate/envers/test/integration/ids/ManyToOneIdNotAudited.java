package org.hibernate.envers.test.integration.ids;

import javax.persistence.EntityManager;

import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.ids.ManyToOneIdNotAuditedTestEntity;
import org.hibernate.envers.test.entities.ids.ManyToOneNotAuditedEmbId;

/**
 * A test checking that when using Envers it is possible to have non-audited entities that use unsupported
 * components in their ids, e.g. a many-to-one join to another entity.
 * @author Adam Warski (adam at warski dot org)
 */
public class ManyToOneIdNotAudited extends BaseEnversJPAFunctionalTestCase {
    private ManyToOneNotAuditedEmbId id1;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(ManyToOneIdNotAuditedTestEntity.class);
        cfg.addAnnotatedClass(UnversionedStrTestEntity.class);
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();

        UnversionedStrTestEntity uste = new UnversionedStrTestEntity();
        uste.setStr("test1");
        em.persist(uste);

        id1 = new ManyToOneNotAuditedEmbId(uste);

        em.getTransaction().commit();

        // Revision 2
        em = getEntityManager();
        em.getTransaction().begin();

        ManyToOneIdNotAuditedTestEntity mtoinate = new ManyToOneIdNotAuditedTestEntity();
        mtoinate.setData("data1");
        mtoinate.setId(id1);
        em.persist(mtoinate);

        em.getTransaction().commit();
    }
}
