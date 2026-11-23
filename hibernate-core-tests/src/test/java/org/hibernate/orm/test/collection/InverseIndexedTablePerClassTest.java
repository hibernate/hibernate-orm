package org.hibernate.orm.test.collection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which writes the index column from
 * {@code TablePerSubclassOneToManyDecomposer}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractInverseIndexedTablePerClassTest.Crate.class,
				AbstractInverseIndexedTablePerClassTest.Item.class,
				AbstractInverseIndexedTablePerClassTest.Thing.class,
				AbstractInverseIndexedTablePerClassTest.ThingA.class,
				AbstractInverseIndexedTablePerClassTest.ThingB.class,
				AbstractInverseIndexedTablePerClassTest.Branch.class,
				AbstractInverseIndexedTablePerClassTest.Twig.class
		}
)
@SessionFactory
public class InverseIndexedTablePerClassTest extends AbstractInverseIndexedTablePerClassTest {
}
