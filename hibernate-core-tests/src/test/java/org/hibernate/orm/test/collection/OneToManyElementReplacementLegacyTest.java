package org.hibernate.orm.test.collection;

import org.hibernate.cfg.FlushSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.Setting;

/**
 * Tests the legacy {@code ActionQueue}, which updates the rows from
 * {@code UpdateRowsCoordinatorOneToMany}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractOneToManyElementReplacementTest.Board.class,
				AbstractOneToManyElementReplacementTest.Piece.class,
				AbstractOneToManyElementReplacementTest.Cell.class,
				AbstractOneToManyElementReplacementTest.Rook.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "legacy")
)
public class OneToManyElementReplacementLegacyTest extends AbstractOneToManyElementReplacementTest {
}
