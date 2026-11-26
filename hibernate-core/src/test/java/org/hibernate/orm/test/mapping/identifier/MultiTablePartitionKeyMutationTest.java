package org.hibernate.orm.test.mapping.identifier;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		AbstractMultiTablePartitionKeyMutationTest.DetailedBox.class,
		AbstractMultiTablePartitionKeyMutationTest.Animal.class,
		AbstractMultiTablePartitionKeyMutationTest.Dog.class
})
@SessionFactory(useCollectingStatementObserver = true)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNullablePartitionKey.class)
@JiraKey("HHH-21279")
public class MultiTablePartitionKeyMutationTest extends AbstractMultiTablePartitionKeyMutationTest {
}
