package org.hibernate.envers.test.integration.merge;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.testing.TestForIssue;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-6753")
public class AddDelTest extends AbstractSessionTest {
    @Override
    protected void initMappings() {
        config.addAnnotatedClass(StrTestEntity.class);
        config.addAnnotatedClass(GivenIdStrEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        // Revision 1
        getSession().getTransaction().begin();
        GivenIdStrEntity entity = new GivenIdStrEntity(1, "data");
        getSession().persist(entity);
        getSession().getTransaction().commit();

        // Revision 2
        getSession().getTransaction().begin();
        getSession().persist(new StrTestEntity("another data")); // Just to create second revision.
        entity = (GivenIdStrEntity) getSession().get(GivenIdStrEntity.class, 1);
        getSession().delete(entity); // First try to remove the entity.
        getSession().save(entity); // Then save it.
        getSession().getTransaction().commit();

        // Revision 3
        getSession().getTransaction().begin();
        entity = (GivenIdStrEntity) getSession().get(GivenIdStrEntity.class, 1);
        getSession().delete(entity); // First try to remove the entity.
        entity.setData("modified data"); // Then change it's state.
        getSession().save(entity); // Finally save it.
        getSession().getTransaction().commit();
    }

    @Test
    public void testRevisionsCountOfGivenIdStrEntity() {
        // Revision 2 has not changed entity's state.
        Assert.assertEquals(Arrays.asList(1, 3), getAuditReader().getRevisions(GivenIdStrEntity.class, 1));
    }

    @Test
    public void testHistoryOfGivenIdStrEntity() {
        Assert.assertEquals(new GivenIdStrEntity(1, "data"), getAuditReader().find(GivenIdStrEntity.class, 1, 1));
        Assert.assertEquals(new GivenIdStrEntity(1, "modified data"), getAuditReader().find(GivenIdStrEntity.class, 1, 3));
    }
}
