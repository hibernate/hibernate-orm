package org.hibernate.envers.test.integration.proxy;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;
import org.hibernate.envers.test.tools.TestTools;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;

@TestForIssue(jiraKey = "HHH-5845")
public class RemovedObjectQueryTest extends BaseEnversJPAFunctionalTestCase {
    @Override
    @SuppressWarnings("unchecked")
    protected void addConfigOptions(Map options) {
        options.put(EnversSettings.STORE_DATA_AT_DELETE, "true");
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { SetRefEdEntity.class, SetRefIngEntity.class };
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();

        SetRefEdEntity refEdEntity = new SetRefEdEntity(1, "Demo Data");
        SetRefIngEntity refIngEntity = new SetRefIngEntity(2, "Example Data", refEdEntity);

        em.getTransaction().begin();
        em.persist(refEdEntity);
        em.persist(refIngEntity);
        em.getTransaction().commit();

        em.getTransaction().begin();
        refIngEntity = em.find(SetRefIngEntity.class, 2);
        em.remove(refIngEntity);
        em.remove(refEdEntity);
        em.getTransaction().commit();
    }

    @Test
    public void testFindDeletedReference() {
        AuditQuery query = getAuditReader().createQuery().forRevisionsOfEntity(SetRefIngEntity.class, false, true)
                                                         .add(AuditEntity.revisionType().eq(RevisionType.DEL));
        List queryResult = (List) query.getResultList();

        Object[] objArray = (Object[]) queryResult.get(0);
        SetRefIngEntity refIngEntity = (SetRefIngEntity) objArray[0];
        Assert.assertEquals("Example Data", refIngEntity.getData());

        Hibernate.initialize(refIngEntity.getReference());
        Assert.assertEquals("Demo Data", refIngEntity.getReference().getData());
    }

    @FailureExpected(jiraKey = "HHH-5845") // TODO: doesn't work until collection queries are fixed
    @Test
    public void testFindDeletedReferring() {
        AuditQuery query = getAuditReader().createQuery().forRevisionsOfEntity(SetRefEdEntity.class, false, true)
                                                         .add(AuditEntity.revisionType().eq(RevisionType.DEL));
        List queryResult = (List) query.getResultList();

        Object[] objArray = (Object[]) queryResult.get(0);
        SetRefEdEntity refEdEntity = (SetRefEdEntity) objArray[0];
        Assert.assertEquals("Demo Data", refEdEntity.getData());

        Hibernate.initialize(refEdEntity.getReffering());
        Assert.assertEquals(TestTools.makeSet(new SetRefIngEntity(2, "Example Data")), refEdEntity.getReffering());
    }
}
