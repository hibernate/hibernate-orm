package org.hibernate.orm.test.secondarytable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		AbstractOptionalSecondaryRowTest.Shipment.class,
		AbstractOptionalSecondaryRowTest.DynamicShipment.class,
		AbstractOptionalSecondaryRowTest.Memo.class
})
@SessionFactory
@JiraKey("HHH-21283")
public class OptionalSecondaryRowTest extends AbstractOptionalSecondaryRowTest {
}
