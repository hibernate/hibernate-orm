package org.hibernate.test.idgen.identity.hhh10429;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.factory.IdentifierGeneratorFactory;
import org.hibernate.mapping.KeyValue;
import org.hibernate.test.annotations.id.entities.Bunny;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.junit.Assert.*;

/**
 * Created by yinzara on 1/14/16.
 */
@TestForIssue(jiraKey = "HHH-10429")
@RequiresDialectFeature( DialectChecks.SupportsIdentityColumns.class )
public class IdentityGeneratorExtendsTest extends BaseCoreFunctionalTestCase {

    @Entity
    @Table
    public static class EntityBean {

        @Id
        @Column
        @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "customGenerator")
        @GenericGenerator(name = "customGenerator", strategy = "org.hibernate.test.idgen.identity.hhh10429.CustomIdentityGenerator")
        private int entityId;

        public int getEntityId() {
            return entityId;
        }

        public void setEntityId(int entityId) {
            this.entityId = entityId;
        }
    }


    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[] { EntityBean.class };
    }

    @Test
    public void testIdentifierGeneratorExtendsIdentityGenerator() {
        final MetadataSources sources  = new MetadataSources(serviceRegistry());
        sources.addAnnotatedClass(EntityBean.class);

        final MetadataBuilder builder = sources.getMetadataBuilder();
        final Metadata metadata = builder.build();

        for (final Namespace ns : metadata.getDatabase().getNamespaces()) {
            for (final org.hibernate.mapping.Table table : ns.getTables()) {
                final KeyValue value = table.getIdentifierValue();
                assertNotNull("IdentifierValue was null", value);
                assertTrue(value.isIdentityColumn(metadata.getIdentifierGeneratorFactory(), getDialect()));
            }
        }
    }
}
