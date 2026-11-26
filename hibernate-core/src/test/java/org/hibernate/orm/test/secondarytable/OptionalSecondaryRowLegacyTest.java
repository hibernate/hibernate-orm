package org.hibernate.orm.test.secondarytable;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		AbstractOptionalSecondaryRowTest.Shipment.class,
		AbstractOptionalSecondaryRowTest.DynamicShipment.class,
		AbstractOptionalSecondaryRowTest.Memo.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
@JiraKey("HHH-21283")
public class OptionalSecondaryRowLegacyTest extends AbstractOptionalSecondaryRowTest {
}
