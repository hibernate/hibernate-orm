package org.hibernate.envers.test.integration.proxy;

import org.hibernate.MappingException;
import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.entities.StrTestEntity;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class QueryingWithProxyObjectTest extends AbstractSessionTest {
    private Integer id = null;

    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        config.addAnnotatedClass(StrTestEntity.class);
    }

    @BeforeClass(dependsOnMethods = "init")
    public void initData() {
        // Revision 1
        newSessionFactory();
        getSession().getTransaction().begin();
        StrTestEntity ste = new StrTestEntity("data");
        getSession().persist(ste);
        getSession().getTransaction().commit();
        id = ste.getId();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testQueryingWithProxyObject() {
        StrTestEntity originalSte = new StrTestEntity("data", id);
        // Load the proxy instance
        StrTestEntity proxySte = (StrTestEntity) getSession().load(StrTestEntity.class, id);

        Assert.assertTrue(getAuditReader().isEntityClassAudited(proxySte.getClass()));

        StrTestEntity ste = getAuditReader().find(proxySte.getClass(), proxySte.getId(), 1);
        Assert.assertEquals(originalSte, ste);

        List<Number> revisions = getAuditReader().getRevisions(proxySte.getClass(), proxySte.getId());
        Assert.assertEquals(Arrays.asList(1), revisions);

        List<StrTestEntity> entities = getAuditReader().createQuery().forEntitiesAtRevision(proxySte.getClass(), 1).getResultList();
        Assert.assertEquals(Arrays.asList(originalSte), entities);

        ste = (StrTestEntity) getAuditReader().createQuery().forRevisionsOfEntity(proxySte.getClass(), true, false).getSingleResult();
        Assert.assertEquals(originalSte, ste);
    }
}
