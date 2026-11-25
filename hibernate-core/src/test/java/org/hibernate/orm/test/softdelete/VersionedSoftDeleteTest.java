package org.hibernate.orm.test.softdelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;

/**
 * Tests the graph-based {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		AbstractVersionedSoftDeleteTest.Item.class,
		AbstractVersionedSoftDeleteTest.Account.class,
		AbstractVersionedSoftDeleteTest.SavingsAccount.class,
		AbstractVersionedSoftDeleteTest.Vehicle.class,
		AbstractVersionedSoftDeleteTest.Car.class
})
@SessionFactory
public class VersionedSoftDeleteTest extends AbstractVersionedSoftDeleteTest {
}
