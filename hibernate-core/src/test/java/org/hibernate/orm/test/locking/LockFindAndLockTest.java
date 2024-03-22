package org.hibernate.orm.test.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.LockModeType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;
import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.id.enhanced.StandardOptimizerDescriptor;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.query.NullPrecedence;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * <p>
 * NOTE: The issue is only reproducible with the {@link BytecodeEnhancerRunner} enabled!
 */
@JiraKey("HHH-17828")
@RunWith(BytecodeEnhancerRunner.class) // This runner enables bytecode enhancement for your test.
public class LockFindAndLockTest extends BaseCoreFunctionalTestCase {
    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{
                MainEntity.class,
                ReferencedEntity.class,
        };
    }

    // Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
    @Override
    protected void configure(Configuration configuration) {
        super.configure(configuration);

        // For your own convenience to see generated queries:
        configuration.setProperty(AvailableSettings.SHOW_SQL, Boolean.TRUE.toString());
        configuration.setProperty(AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString());
        //configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );

        // Other settings that will make your test case run under similar configuration that Quarkus is using by default:
        //
        // NOTE: These settings seem to be irrelevant for the bug, but I still left them there to be as close to quarkus as possible
        // =========================================================================================================================
        configuration.setProperty(AvailableSettings.PREFERRED_POOLED_OPTIMIZER, StandardOptimizerDescriptor.POOLED_LO.getExternalName());
        configuration.setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "16");
        configuration.setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
        configuration.setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, "2048");
        configuration.setProperty(AvailableSettings.DEFAULT_NULL_ORDERING, NullPrecedence.NONE.toString().toLowerCase(Locale.ROOT));
        configuration.setProperty(AvailableSettings.IN_CLAUSE_PARAMETER_PADDING, "true");
        configuration.setProperty(AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY, SequenceMismatchStrategy.NONE.toString());

        // Add your own settings that are a part of your quarkus configuration:
        // configuration.setProperty( AvailableSettings.SOME_CONFIGURATION_PROPERTY, "SOME_VALUE" );
    }


    @Test
    public void testFindAndLockAfterLock() {
        inTransaction(
                session -> {
                    final ReferencedEntity e1 = new ReferencedEntity(0L);
                    session.persist(e1);
                    session.persist(new MainEntity(0L, e1));
                }
        );

        inTransaction(
                session -> {
                    // First find and lock the main entity
                    MainEntity m = session.find(MainEntity.class, 0L, LockModeType.PESSIMISTIC_WRITE);
                    assertNotNull(m);
                    ReferencedEntity lazyReference = m.referencedLazy();
                    assertNotNull(lazyReference);
                    assertFalse(Hibernate.isInitialized(lazyReference));

                    // Then find and lock the referenced entity
                    ReferencedEntity lazyEntity = session.find(ReferencedEntity.class, 0L, LockModeType.PESSIMISTIC_WRITE);
                    assertNotNull(lazyEntity);

                    assertEquals(LockModeType.PESSIMISTIC_WRITE, session.getLockMode(lazyEntity));
                }
        );
    }

    @Entity
    public static class MainEntity {
        @Id
        private long id;

        @Version
        private long tanum;

        @ManyToOne(targetEntity = ReferencedEntity.class, fetch = FetchType.LAZY)
        @JoinColumn(name = "LAZY_COLUMN")
        private ReferencedEntity referencedLazy;

        protected MainEntity() {
        }

        public MainEntity(long id, ReferencedEntity lazy) {
            this.id = id;
            this.referencedLazy = lazy;
        }

        public ReferencedEntity referencedLazy() {
            return referencedLazy;
        }
    }

    @Entity
    public static class ReferencedEntity {

        @Id
        private long id;

        @Version
        private long tanum;

        protected ReferencedEntity() {
        }

        public ReferencedEntity(long id) {
            this.id = id;
        }
    }
}
