package org.hibernate.orm.test.collection;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}, which writes the rows from the
 * {@code TablePerSubclass} row coordinators.
 */
@DomainModel(
		annotatedClasses = {
				AbstractOwnedMapTablePerClassTest.Box.class,
				AbstractOwnedMapTablePerClassTest.Item.class,
				AbstractOwnedMapTablePerClassTest.Thing.class,
				AbstractOwnedMapTablePerClassTest.ThingA.class,
				AbstractOwnedMapTablePerClassTest.ThingB.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class OwnedMapTablePerClassLegacyTest extends AbstractOwnedMapTablePerClassTest {
}
