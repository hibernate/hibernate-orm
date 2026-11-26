package org.hibernate.orm.test.mapping.identifier;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		AbstractPartitionKeyMutationTest.Box.class,
		AbstractPartitionKeyMutationTest.VersionedBox.class,
		AbstractPartitionKeyMutationTest.CustomDeleteBox.class,
		AbstractPartitionKeyMutationTest.Vehicle.class,
		AbstractPartitionKeyMutationTest.Truck.class,
		AbstractPartitionKeyMutationTest.SealedCarton.class
})
@SessionFactory(useCollectingStatementObserver = true)
@JiraKey("HHH-21272")
public class PartitionKeyMutationTest extends AbstractPartitionKeyMutationTest {
}
