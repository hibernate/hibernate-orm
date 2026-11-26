package org.hibernate.orm.test.softdelete;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		PartitionKeyNotLoadedTest.LazyShipment.class,
		PartitionKeyNotLoadedTest.LazyParcel.class,
		PartitionKeyNotLoadedTest.LazyCrate.class,
		PartitionKeyNotLoadedTest.Pallet.class,
		PartitionKeyNotLoadedTest.Container.class,
		PartitionKeyNotLoadedTest.CustomDeleteContainer.class
})
@SessionFactory
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-21279")
class PartitionKeyNotLoadedLegacyTest extends PartitionKeyNotLoadedTest {
}
