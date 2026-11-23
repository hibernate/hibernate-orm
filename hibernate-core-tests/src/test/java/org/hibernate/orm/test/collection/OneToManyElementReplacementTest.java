package org.hibernate.orm.test.collection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which plans the row changes in
 * {@code AbstractOneToManyDecomposer}.
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
public class OneToManyElementReplacementTest extends AbstractOneToManyElementReplacementTest {
}
