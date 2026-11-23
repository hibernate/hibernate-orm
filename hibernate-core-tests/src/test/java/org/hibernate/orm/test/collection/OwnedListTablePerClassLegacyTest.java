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
				AbstractOwnedListTablePerClassTest.Box.class,
				AbstractOwnedListTablePerClassTest.Item.class,
				AbstractOwnedListTablePerClassTest.Thing.class,
				AbstractOwnedListTablePerClassTest.ThingA.class,
				AbstractOwnedListTablePerClassTest.ThingB.class,
				AbstractOwnedListTablePerClassTest.Branch.class,
				AbstractOwnedListTablePerClassTest.Twig.class,
				AbstractOwnedListTablePerClassTest.Label.class,
				AbstractOwnedListTablePerClassTest.Shelf.class,
				AbstractOwnedListTablePerClassTest.Tool.class,
				AbstractOwnedListTablePerClassTest.PowerTool.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class OwnedListTablePerClassLegacyTest extends AbstractOwnedListTablePerClassTest {
}
