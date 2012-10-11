package org.hibernate.envers.test.integration.basic;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.entities.IntTestEntity;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author Tomasz Dziurko (tdziurko at gmail dot com)
 */
public class TransactionRollbackBehaviour  extends AbstractEntityTest {

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(IntTestEntity.class);
    }

    @Test
    public void testAuditRecordsRollback() {
        // Given
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        IntTestEntity iteToRollback = new IntTestEntity(30);
        em.persist(iteToRollback);
        Integer rollbackedIteId = iteToRollback.getId();
        em.getTransaction().rollback();

        // When
        em.getTransaction().begin();
        IntTestEntity ite2 = new IntTestEntity(50);
        em.persist(ite2);
        Integer ite2Id = ite2.getId();
        em.getTransaction().commit();

        // Then
        List<Number> revisionsForSavedClass = getAuditReader().getRevisions(IntTestEntity.class, ite2Id);
        assertEquals(revisionsForSavedClass.size(), 1, "There should be one revision for inserted entity");

        List<Number> revisionsForRolledbackClass = getAuditReader().getRevisions(IntTestEntity.class, rollbackedIteId);
        assertEquals(revisionsForRolledbackClass.size(), 0, "There should be no revisions for insert that was rolled back");
    }
}
