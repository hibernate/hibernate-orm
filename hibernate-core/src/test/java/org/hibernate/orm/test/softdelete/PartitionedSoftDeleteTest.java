package org.hibernate.orm.test.softdelete;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the graph-based {@code ActionQueue}.
 */
@DomainModel(annotatedClasses = {
		AbstractPartitionedSoftDeleteTest.Shipment.class,
		AbstractPartitionedSoftDeleteTest.Parcel.class,
		AbstractPartitionedSoftDeleteTest.Invoice.class
})
@SessionFactory(useCollectingStatementObserver = true)
public class PartitionedSoftDeleteTest extends AbstractPartitionedSoftDeleteTest {
	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNullablePartitionKey.class)
	void removeInstanceWithNullPartitionKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Parcel( 2, null ) ) );

		final var statements = scope.getCollectingStatementObserver();
		statements.clear();
		scope.inTransaction( session -> session.remove( session.find( Parcel.class, 2 ) ) );
		assertThat( statements.getSqlQueries() ).last().asString().contains( "region is null" );
		assertSoftDeleted( scope, Parcel.class, "partitioned_soft_parcel", 2 );
	}

	/**
	 * An instance which was never loaded has no partition key to restrict by.
	 */
	@Test
	void removeUnloadedInstance(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.remove( session.getReference( Shipment.class, 1 ) ) );
		assertSoftDeleted( scope, Shipment.class, "partitioned_soft_shipment", 1 );
	}

}
