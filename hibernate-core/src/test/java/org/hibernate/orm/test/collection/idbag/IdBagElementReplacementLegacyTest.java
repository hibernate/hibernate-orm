package org.hibernate.orm.test.collection.idbag;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}, which updates the rows from
 * {@code UpdateRowsCoordinatorStandard}.
 */
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class IdBagElementReplacementLegacyTest extends AbstractIdBagElementReplacementTest {
}
