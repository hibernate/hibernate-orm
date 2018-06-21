package org.hibernate.jpa.test.callbacks;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.*;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@TestForIssue( jiraKey = "HHH-12718" )
@RunWith(BytecodeEnhancerRunner.class)
public class PreUpdateModificationsLazyLoadingTest extends BaseEntityManagerFunctionalTestCase {

    @Test
    public void testPreUpdateModifications() {
        EntityManager em = getOrCreateEntityManager();
        em.getTransaction().begin();
        Person p = new Person();
        em.persist(p);
        em.getTransaction().commit();

        em.clear();

        em.getTransaction().begin();
        p = em.find(Person.class, p.id);
        assertNotNull(p);
        assertNotNull(p.createdAt);
        assertNull(p.lastUpdatedAt);

        p.name = "Changed Name";
        em.getTransaction().commit();

        em.clear();

        p = em.find(Person.class, p.id);
        // This last assertion fails
        assertNotNull(p.lastUpdatedAt);
        em.close();
    }

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] { Person.class };
    }

    @Override
    protected void addConfigOptions(Map options) {
        options.put(AvailableSettings.CLASSLOADERS, getClass().getClassLoader() );
        options.put(AvailableSettings.ENHANCER_ENABLE_LAZY_INITIALIZATION, "true");
        options.put(AvailableSettings.ENHANCER_ENABLE_DIRTY_TRACKING, "true");
    }

    @Entity(name = "Person")
    private static class Person {
        @Id
        @GeneratedValue
        private int id;

        private String name;

        private Instant createdAt;

        private Instant lastUpdatedAt;

        @ElementCollection
        private List<String> tags;

        @Lob
        @Basic(fetch = FetchType.LAZY)
        private ByteBuffer image;

        @PrePersist
        void beforeCreate() {
            createdAt = Instant.now();
        }

        @PreUpdate
        void beforeUpdate() {
            lastUpdatedAt = Instant.now();
        }
    }
}
