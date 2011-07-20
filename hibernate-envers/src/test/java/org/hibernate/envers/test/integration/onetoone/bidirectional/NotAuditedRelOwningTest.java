package org.hibernate.envers.test.integration.onetoone.bidirectional;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NotAuditedRelOwningTest extends AbstractEntityTest {
    private Long relOwnedId = 1L;
    private Long relOwnedNoProxyId = 2L;
    private Long relOwningId = 3L;
    private Long relOwningNoProxyId = 4L;

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(AuditedRelOwnedEntity.class);
        cfg.addAnnotatedClass(AuditedNoProxyRelOwnedEntity.class);
        cfg.addAnnotatedClass(NotAuditedRelOwningEntity.class);
        cfg.addAnnotatedClass(NotAuditedNoProxyRelOwningEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();

        // Revision 1
        em.getTransaction().begin();
        AuditedRelOwnedEntity auditedRelOwned = new AuditedRelOwnedEntity(relOwnedId, "audited data", null);
        NotAuditedRelOwningEntity notAuditedRelOwning = new NotAuditedRelOwningEntity(relOwningId, "invalid data", auditedRelOwned);
        auditedRelOwned.setTarget(notAuditedRelOwning);
        em.persist(auditedRelOwned);
        em.persist(notAuditedRelOwning);
        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();
        AuditedNoProxyRelOwnedEntity auditedNoProxyRelOwned = new AuditedNoProxyRelOwnedEntity(relOwnedNoProxyId, "audited no proxy data", null);
        NotAuditedNoProxyRelOwningEntity notAuditedNoProxyRelOwning = new NotAuditedNoProxyRelOwningEntity(relOwningNoProxyId, "invalid data", auditedNoProxyRelOwned);
        auditedNoProxyRelOwned.setTarget(notAuditedNoProxyRelOwning);
        em.persist(auditedNoProxyRelOwned);
        em.persist(notAuditedNoProxyRelOwning);
        em.getTransaction().commit();

        // Modify relation owning entities.
        em.getTransaction().begin();
        notAuditedRelOwning = em.find(NotAuditedRelOwningEntity.class, relOwningId);
        notAuditedRelOwning.setData("not audited data");
        notAuditedNoProxyRelOwning = em.find(NotAuditedNoProxyRelOwningEntity.class, relOwningNoProxyId);
        notAuditedNoProxyRelOwning.setData("not audited no proxy data");
        em.getTransaction().commit();
    }

    @Test
    @TestForIssue(jiraKey="HHH-6317")
    public void testOwningProxyObject() {
        // AuditedRelOwnedEntity.equals() method omits checking "target" attribute.
        AuditedRelOwnedEntity expected = new AuditedRelOwnedEntity(relOwnedId, "audited data", null);
        
        AuditedRelOwnedEntity ver1 = getAuditReader().find(AuditedRelOwnedEntity.class, relOwnedId, 1);

        Assert.assertEquals(expected, ver1);
        Assert.assertTrue(ver1.getTarget() instanceof HibernateProxy);
        Assert.assertEquals(relOwningId, ver1.getTarget().getId());
    }

    @Test
    @TestForIssue(jiraKey="HHH-6317")
    public void testOwningNoProxyObject() {
        // AuditedNoProxyRelOwnedEntity.equals() method omits checking "target" attribute.
        AuditedNoProxyRelOwnedEntity expected = new AuditedNoProxyRelOwnedEntity(relOwnedNoProxyId, "audited no proxy data", null);
        // NotAuditedNoProxyRelOwningEntity.equals() method omits checking "reference" attribute.
        NotAuditedNoProxyRelOwningEntity expectedNoProxy = new NotAuditedNoProxyRelOwningEntity(relOwningNoProxyId, "not audited no proxy data", null);

        AuditedNoProxyRelOwnedEntity ver2 = getAuditReader().find(AuditedNoProxyRelOwnedEntity.class, relOwnedNoProxyId, 2);

        Assert.assertEquals(expected, ver2);
        Assert.assertFalse(ver2.getTarget() instanceof HibernateProxy);
        Assert.assertEquals(expectedNoProxy, ver2.getTarget());
    }
}
