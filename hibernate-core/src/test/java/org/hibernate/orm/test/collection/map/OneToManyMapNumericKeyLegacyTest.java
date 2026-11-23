package org.hibernate.orm.test.collection.map;

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
				AbstractOneToManyMapNumericKeyTest.Board.class,
				AbstractOneToManyMapNumericKeyTest.Tile.class,
				AbstractOneToManyMapNumericKeyTest.Pawn.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class OneToManyMapNumericKeyLegacyTest extends AbstractOneToManyMapNumericKeyTest {
}
