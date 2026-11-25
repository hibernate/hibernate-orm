package org.hibernate.orm.test.softdelete;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

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
	/**
	 * An instance which was never loaded has no version to check.
	 */
	@Test
	void removeUnloadedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.getReference( Item.class, 1 ) ) );
		assertSoftDeleted( scope, Item.class, "versioned_soft_item" );
	}
}
