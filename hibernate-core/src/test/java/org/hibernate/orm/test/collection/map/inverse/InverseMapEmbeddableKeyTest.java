package org.hibernate.orm.test.collection.map.inverse;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which writes the map key columns from
 * {@code AbstractOneToManyDecomposer}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractInverseMapEmbeddableKeyTest.Board.class,
				AbstractInverseMapEmbeddableKeyTest.Cell.class
		}
)
@SessionFactory
public class InverseMapEmbeddableKeyTest extends AbstractInverseMapEmbeddableKeyTest {
}
