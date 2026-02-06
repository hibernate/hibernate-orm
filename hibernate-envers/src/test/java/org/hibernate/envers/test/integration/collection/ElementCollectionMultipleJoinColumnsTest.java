package org.hibernate.envers.test.integration.collection;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.entities.collection.ElementCollectionMultipleJoinColumnEntity;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@TestForIssue(jiraKey = "HHH-14263")
public class ElementCollectionMultipleJoinColumnsTest extends BaseEnversJPAFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { ElementCollectionMultipleJoinColumnEntity.class };
    }

    @Test
    public void testAuditForElementCollectionMultipleJoinColumnsWorks() {
        AuditReader reader = getAuditReader();
        EntityManager em = getEntityManager();
        Set<String> barIds = new HashSet<>();
        barIds.add("bar1");
        em.getTransaction().begin();
        ElementCollectionMultipleJoinColumnEntity entity = new ElementCollectionMultipleJoinColumnEntity();
        entity.setFooId("foo1");
        entity.setBarIds(new HashSet<>(barIds));
        entity.setBazId("baz1");
        em.persist(entity);
        em.getTransaction().commit();

        Set<String> barIds2 = new HashSet<>();
        barIds2.add("bar1");
        barIds2.add("bar2");
        em.getTransaction().begin();
        ElementCollectionMultipleJoinColumnEntity entity2 = new ElementCollectionMultipleJoinColumnEntity();
        entity2.setFooId("foo1");
        entity2.setBarIds(new HashSet<>(barIds2));
        entity2.setBazId("baz1");
        em.merge(entity2);
        em.getTransaction().commit();

        assertEquals(barIds, reader.find(ElementCollectionMultipleJoinColumnEntity.class, "foo1", 1).getBarIds());
        assertEquals(barIds2, reader.find(ElementCollectionMultipleJoinColumnEntity.class, "foo1", 2).getBarIds());
    }
}
