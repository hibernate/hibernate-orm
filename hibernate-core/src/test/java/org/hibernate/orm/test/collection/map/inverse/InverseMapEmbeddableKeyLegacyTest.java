package org.hibernate.orm.test.collection.map.inverse;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}, which writes the map key columns from
 * {@code WriteIndexCoordinatorStandard}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractInverseMapEmbeddableKeyTest.Board.class,
				AbstractInverseMapEmbeddableKeyTest.Cell.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class InverseMapEmbeddableKeyLegacyTest extends AbstractInverseMapEmbeddableKeyTest {
}
