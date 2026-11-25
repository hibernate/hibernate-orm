package org.hibernate.orm.test.collection.ordercol;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * {@link NonUpdatableJoinColumnOrderColumnTest} with the legacy action queue.
 */
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
class NonUpdatableJoinColumnOrderColumnLegacyTest extends NonUpdatableJoinColumnOrderColumnTest {
}
