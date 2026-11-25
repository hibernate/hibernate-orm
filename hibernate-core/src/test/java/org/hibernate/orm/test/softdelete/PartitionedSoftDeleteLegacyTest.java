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
		AbstractPartitionedSoftDeleteTest.Shipment.class,
		AbstractPartitionedSoftDeleteTest.Parcel.class,
		AbstractPartitionedSoftDeleteTest.Invoice.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
public class PartitionedSoftDeleteLegacyTest extends AbstractPartitionedSoftDeleteTest {
}
