package org.hibernate.orm.test.collection.map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which plans the row changes in
 * {@code AbstractOneToManyDecomposer}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractOneToManyMapNumericKeyTest.Board.class,
				AbstractOneToManyMapNumericKeyTest.Tile.class,
				AbstractOneToManyMapNumericKeyTest.Pawn.class
		}
)
@SessionFactory
public class OneToManyMapNumericKeyTest extends AbstractOneToManyMapNumericKeyTest {
}
