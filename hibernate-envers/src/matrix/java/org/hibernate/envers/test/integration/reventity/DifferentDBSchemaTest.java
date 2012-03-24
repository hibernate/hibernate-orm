package org.hibernate.envers.test.integration.reventity;

import java.util.Arrays;
import java.util.Properties;
import javax.persistence.EntityManager;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.AbstractEntityTest;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.mapping.Table;

/**
 * Tests simple auditing process (read and write operations) when <i>REVINFO</i> and audit tables
 * exist in a different database schema.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialect({H2Dialect.class})
public class DifferentDBSchemaTest extends AbstractEntityTest {
    private static final String SCHEMA_NAME = "ENVERS_AUDIT";
    private Integer steId = null;

    @Override
    public void addConfigurationProperties(Properties configuration) {
        super.addConfigurationProperties(configuration);

        // Creates new schema after establishing connection
        configuration.setProperty(Environment.URL, configuration.getProperty(Environment.URL) + ";INIT=CREATE SCHEMA IF NOT EXISTS " + SCHEMA_NAME);
        configuration.setProperty("org.hibernate.envers.default_schema", SCHEMA_NAME);
    }

    @Override
    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(StrTestEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() throws InterruptedException {
        // Revision 1
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        StrTestEntity ste = new StrTestEntity("x");
        em.persist(ste);
        steId = ste.getId();
        em.getTransaction().commit();

        // Revision 2
        em.getTransaction().begin();
        ste = em.find(StrTestEntity.class, steId);
        ste.setStr("y");
        em.getTransaction().commit();
    }

    @Test
    public void testRevinfoSchemaName() {
        Table revisionTable = getCfg().getClassMapping("org.hibernate.envers.DefaultRevisionEntity").getTable();
        assert SCHEMA_NAME.equals(revisionTable.getSchema());
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2).equals(getAuditReader().getRevisions(StrTestEntity.class, steId));
    }

    @Test
    public void testHistoryOfId1() {
        StrTestEntity ver1 = new StrTestEntity("x", steId);
        StrTestEntity ver2 = new StrTestEntity("y", steId);

        assert getAuditReader().find(StrTestEntity.class, steId, 1).equals(ver1);
        assert getAuditReader().find(StrTestEntity.class, steId, 2).equals(ver2);
    }
}
