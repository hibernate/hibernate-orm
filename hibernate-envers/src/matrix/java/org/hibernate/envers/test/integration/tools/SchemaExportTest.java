package org.hibernate.envers.test.integration.tools;

import java.net.URISyntaxException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.envers.test.AbstractSessionTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.testing.TestForIssue;
import org.hibernate.tool.EnversSchemaGenerator;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7106")
public class SchemaExportTest extends AbstractSessionTest {
    private Integer id = null;

    @Override
    protected void initMappings() throws MappingException, URISyntaxException {
        config.addAnnotatedClass(StrTestEntity.class);
        // Disable schema auto generation.
        config.setProperty(Environment.HBM2DDL_AUTO, "");
    }

    @Test
    @Priority(10)
    public void testSchemaCreation() {
        // Generate complete schema.
        new EnversSchemaGenerator(config).export().create(true, true);

        // Populate database with test data.
        Session session = getSession();
        session.getTransaction().begin();
        StrTestEntity entity = new StrTestEntity("data");
        session.save(entity);
        session.getTransaction().commit();

        id = entity.getId();
    }

    @Test
    public void testAuditDataRetrieval() {
        Assert.assertEquals(Arrays.asList(1), getAuditReader().getRevisions(StrTestEntity.class, id));
        Assert.assertEquals(new StrTestEntity("data", id), getAuditReader().find(StrTestEntity.class, id, 1));
    }
}
