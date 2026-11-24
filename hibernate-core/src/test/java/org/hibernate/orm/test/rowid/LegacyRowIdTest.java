package org.hibernate.orm.test.rowid;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;

@ServiceRegistry(settings = @Setting(name = FLUSH_QUEUE_TYPE, value = "legacy"))
class LegacyRowIdTest extends RowIdTest {
}
