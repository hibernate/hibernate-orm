package org.hibernate.orm.test.collection.idbag;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractIdBagAddAndRemoveTest.Student.class,
				AbstractIdBagAddAndRemoveTest.Course.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class IdBagAddAndRemoveLegacyTest extends AbstractIdBagAddAndRemoveTest {
}
