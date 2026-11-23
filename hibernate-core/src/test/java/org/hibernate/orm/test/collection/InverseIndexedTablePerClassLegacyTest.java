package org.hibernate.orm.test.collection;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}, which writes the index column from
 * {@code WriteIndexCoordinatorTablePerSubclass}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractInverseIndexedTablePerClassTest.Crate.class,
				AbstractInverseIndexedTablePerClassTest.Item.class,
				AbstractInverseIndexedTablePerClassTest.Thing.class,
				AbstractInverseIndexedTablePerClassTest.ThingA.class,
				AbstractInverseIndexedTablePerClassTest.ThingB.class,
				AbstractInverseIndexedTablePerClassTest.Branch.class,
				AbstractInverseIndexedTablePerClassTest.Twig.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class InverseIndexedTablePerClassLegacyTest extends AbstractInverseIndexedTablePerClassTest {
}
