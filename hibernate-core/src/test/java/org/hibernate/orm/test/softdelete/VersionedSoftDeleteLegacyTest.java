package org.hibernate.orm.test.softdelete;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		AbstractVersionedSoftDeleteTest.Item.class,
		AbstractVersionedSoftDeleteTest.Account.class,
		AbstractVersionedSoftDeleteTest.SavingsAccount.class,
		AbstractVersionedSoftDeleteTest.Vehicle.class,
		AbstractVersionedSoftDeleteTest.Car.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
public class VersionedSoftDeleteLegacyTest extends AbstractVersionedSoftDeleteTest {
}
