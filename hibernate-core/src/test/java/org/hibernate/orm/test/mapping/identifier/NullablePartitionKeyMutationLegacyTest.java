package org.hibernate.orm.test.mapping.identifier;

import org.hibernate.cfg.FlushSettings;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		NullablePartitionKeyMutationTest.Crate.class,
		NullablePartitionKeyMutationTest.DynamicCrate.class,
		NullablePartitionKeyMutationTest.SecondaryCrate.class,
		NullablePartitionKeyMutationTest.VersionedCrate.class
})
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNullablePartitionKey.class)
@JiraKey("HHH-21279")
public class NullablePartitionKeyMutationLegacyTest extends NullablePartitionKeyMutationTest {
}
