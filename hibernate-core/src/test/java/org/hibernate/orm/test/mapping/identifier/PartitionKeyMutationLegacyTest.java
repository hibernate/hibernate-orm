package org.hibernate.orm.test.mapping.identifier;

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
		AbstractPartitionKeyMutationTest.Box.class,
		AbstractPartitionKeyMutationTest.VersionedBox.class,
		AbstractPartitionKeyMutationTest.CustomDeleteBox.class,
		AbstractPartitionKeyMutationTest.Vehicle.class,
		AbstractPartitionKeyMutationTest.Truck.class,
		AbstractPartitionKeyMutationTest.Carton.class,
		AbstractPartitionKeyMutationTest.SealedCarton.class,
		AbstractPartitionKeyMutationTest.Tag.class,
		AbstractPartitionKeyMutationTest.Pet.class,
		AbstractPartitionKeyMutationTest.Parrot.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
@JiraKey("HHH-21279")
public class PartitionKeyMutationLegacyTest extends AbstractPartitionKeyMutationTest {
}
