package org.hibernate.orm.test.locking;

import jakarta.persistence.*;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-17395")
public class LockRefreshReferencedTest extends BaseEntityManagerFunctionalTestCase {

    // Add your entities here.
    @Override
    protected Class[] getAnnotatedClasses() {
        return new Class[] {
                MainEntity.class,
                ReferencedEntity.class
        };
    }

    @Override
    protected void addConfigOptions(Map options) {
        options.put( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
        options.put( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
    }


    @Test
    public void testRefreshBeforeRead() {
        doInJPA(this::entityManagerFactory, em -> {
            final ReferencedEntity e1 = new ReferencedEntity(0, "ok");
            final ReferencedEntity e2 = new ReferencedEntity(1, "warn");
            em.persist(e1);
            em.persist(e2);
            final MainEntity e3 = new MainEntity(0, e1, e2);
            em.persist(e3);
        });

        doInJPA(this::entityManagerFactory, em -> {
            MainEntity m = em.find(MainEntity.class, 0);
            assertNotNull(m);
            ReferencedEntity e1 = m.referencedLazy();
            ReferencedEntity e2 = m.referencedEager();
            assertNotNull(e1);
            assertNotNull(e2);

            // First refresh, then access
            em.refresh(e1, LockModeType.PESSIMISTIC_WRITE);
            em.refresh(e2, LockModeType.PESSIMISTIC_WRITE);
            assertEquals("ok", e1.status());
            assertEquals("warn", e2.status());
            assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(e1));
            assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(e2));
        });
    }

    @Test
    public void testRefreshAfterRead() {
        doInJPA(this::entityManagerFactory, em -> {
            final ReferencedEntity e1 = new ReferencedEntity(0, "ok");
            final ReferencedEntity e2 = new ReferencedEntity(1, "warn");
            em.persist(e1);
            em.persist(e2);
            final MainEntity e3 = new MainEntity(0, e1, e2);
            em.persist(e3);
        });

        doInJPA(this::entityManagerFactory, em -> {
            MainEntity m = em.find(MainEntity.class, 0);
            assertNotNull(m);
            ReferencedEntity e1 = m.referencedLazy();
            ReferencedEntity e2 = m.referencedEager();
            assertNotNull(e1);
            assertNotNull(e2);

            // First refresh, then access
            assertEquals("ok", e1.status());
            assertEquals("warn", e2.status());
            em.refresh(e1, LockModeType.PESSIMISTIC_WRITE);
            em.refresh(e2, LockModeType.PESSIMISTIC_WRITE);
            assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(e1));
            assertEquals(LockModeType.PESSIMISTIC_WRITE, em.getLockMode(e2));
        });
    }


    @Entity
    public static class MainEntity {
        @Id
        private long id;

        @OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.LAZY)
        @JoinColumn(name = "LAZY")
        private ReferencedEntity referencedLazy;

        @OneToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.EAGER)
        @JoinColumn(name = "EAGER")
        private ReferencedEntity referencedEager;

        protected MainEntity() {
        }

        public MainEntity(long id, ReferencedEntity lazy, ReferencedEntity eager) {
            this.id = id;
            this.referencedLazy = lazy;
            this.referencedEager = eager;
        }

        public ReferencedEntity referencedLazy() {
            return referencedLazy;
        }

        public ReferencedEntity referencedEager() {
            return referencedEager;
        }
    }

    @Entity
    public static class ReferencedEntity {

        @Id
        private long id;

        private String status;

        protected ReferencedEntity() {
        }

        public ReferencedEntity(long id, String status) {
            this.id = id;
            this.status = status;
        }

        public String status() {
            return status;
        }
    }

}
