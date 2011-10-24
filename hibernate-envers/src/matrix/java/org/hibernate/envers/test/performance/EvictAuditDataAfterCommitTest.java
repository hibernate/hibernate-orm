package org.hibernate.envers.test.performance;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class EvictAuditDataAfterCommitTest extends AbstractSessionTest {
    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        config.addAnnotatedClass(StrTestEntity.class);
        config.addAnnotatedClass(SetRefEdEntity.class);
        config.addAnnotatedClass(SetRefIngEntity.class);
    }

    @Test
    @TestForIssue(jiraKey = "HHH-6614")
    public void testSessionCacheClear() {
        getSession().getTransaction().begin();
        StrTestEntity ste = new StrTestEntity("data");
        getSession().persist(ste);
        getSession().getTransaction().commit();
        checkEmptyAuditSessionCache(getSession(), "org.hibernate.envers.test.entities.StrTestEntity_AUD");
    }

    @Test
    @TestForIssue(jiraKey = "HHH-6614")
    public void testSessionCacheCollectionClear() {
        final String[] auditEntityNames = new String[] {"org.hibernate.envers.test.entities.onetomany.SetRefEdEntity_AUD",
                                                        "org.hibernate.envers.test.entities.onetomany.SetRefIngEntity_AUD"};

        SetRefEdEntity ed1 = new SetRefEdEntity(1, "data_ed_1");
        SetRefEdEntity ed2 = new SetRefEdEntity(2, "data_ed_2");
        SetRefIngEntity ing1 = new SetRefIngEntity(3, "data_ing_1");
        SetRefIngEntity ing2 = new SetRefIngEntity(4, "data_ing_2");
        
        getSession().getTransaction().begin();
        getSession().persist(ed1);
        getSession().persist(ed2);
        getSession().persist(ing1);
        getSession().persist(ing2);
        getSession().getTransaction().commit();
        checkEmptyAuditSessionCache(getSession(), auditEntityNames);

        getSession().getTransaction().begin();
        ed1 = (SetRefEdEntity) getSession().load(SetRefEdEntity.class, ed1.getId());
        ing1.setReference(ed1);
        ing2.setReference(ed1);
        getSession().getTransaction().commit();
        checkEmptyAuditSessionCache(getSession(), auditEntityNames);

        getSession().getTransaction().begin();
        ed2 = (SetRefEdEntity) getSession().load(SetRefEdEntity.class, ed2.getId());
        Set<SetRefIngEntity> reffering = new HashSet<SetRefIngEntity>();
        reffering.add(ing1);
        reffering.add(ing2);
        ed2.setReffering(reffering);
        getSession().getTransaction().commit();
        checkEmptyAuditSessionCache(getSession(), auditEntityNames);

        getSession().getTransaction().begin();
        ed2 = (SetRefEdEntity) getSession().load(SetRefEdEntity.class, ed2.getId());
        ed2.getReffering().remove(ing1);
        getSession().getTransaction().commit();
        checkEmptyAuditSessionCache(getSession(), auditEntityNames);

        getSession().getTransaction().begin();
        ed2 = (SetRefEdEntity) getSession().load(SetRefEdEntity.class, ed2.getId());
        ed2.getReffering().iterator().next().setData("mod_data_ing_2");
        getSession().getTransaction().commit();
        checkEmptyAuditSessionCache(getSession(), auditEntityNames);
    }

    private void checkEmptyAuditSessionCache(Session session, String ... auditEntityNames) {
        List<String> entityNames = Arrays.asList(auditEntityNames);
        PersistenceContext persistenceContext = ((SessionImplementor) session).getPersistenceContext();
        for (Object entry : persistenceContext.getEntityEntries().values()) {
            EntityEntry entityEntry = (EntityEntry) entry;
            if (entityNames.contains(entityEntry.getEntityName())) {
                assert false : "Audit data shall not be stored in the session level cache. This causes performance issues.";
            }
            Assert.assertFalse("Revision entity shall not be stored in the session level cache. This causes performance issues.",
                               DefaultRevisionEntity.class.getName().equals(entityEntry.getEntityName()));
        }
    }
}