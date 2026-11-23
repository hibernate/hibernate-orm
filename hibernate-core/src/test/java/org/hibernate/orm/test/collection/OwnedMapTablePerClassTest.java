package org.hibernate.orm.test.collection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which writes the rows from
 * {@code TablePerSubclassOneToManyDecomposer}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractOwnedMapTablePerClassTest.Box.class,
				AbstractOwnedMapTablePerClassTest.Item.class,
				AbstractOwnedMapTablePerClassTest.Thing.class,
				AbstractOwnedMapTablePerClassTest.ThingA.class,
				AbstractOwnedMapTablePerClassTest.ThingB.class
		}
)
@SessionFactory
public class OwnedMapTablePerClassTest extends AbstractOwnedMapTablePerClassTest {
}
