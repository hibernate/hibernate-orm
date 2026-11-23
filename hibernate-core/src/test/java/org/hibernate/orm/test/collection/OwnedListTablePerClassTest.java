package org.hibernate.orm.test.collection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}, which writes the rows from
 * {@code TablePerSubclassOneToManyDecomposer}.
 */
@DomainModel(
		annotatedClasses = {
				AbstractOwnedListTablePerClassTest.Box.class,
				AbstractOwnedListTablePerClassTest.Item.class,
				AbstractOwnedListTablePerClassTest.Thing.class,
				AbstractOwnedListTablePerClassTest.ThingA.class,
				AbstractOwnedListTablePerClassTest.ThingB.class,
				AbstractOwnedListTablePerClassTest.Branch.class,
				AbstractOwnedListTablePerClassTest.Twig.class,
				AbstractOwnedListTablePerClassTest.Label.class,
				AbstractOwnedListTablePerClassTest.Shelf.class,
				AbstractOwnedListTablePerClassTest.Tool.class,
				AbstractOwnedListTablePerClassTest.PowerTool.class
		}
)
@SessionFactory
public class OwnedListTablePerClassTest extends AbstractOwnedListTablePerClassTest {
}
