package org.hibernate.orm.test.collection.map;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey("HHH-21238")
@BytecodeEnhanced
@DomainModel(annotatedClasses = LazyOwnerCollectionKeyTest.Warehouse.class)
@SessionFactory(useCollectingStatementInspector = true)
class LazyOwnerCollectionKeyTest {
	@Test
	void insertWithUnfetchedOwnerKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var warehouse = new Warehouse();
			warehouse.id = 1;
			warehouse.code = "OWNER";
			session.persist( warehouse );
		} );
		scope.inTransaction( session -> {
			final var warehouse = session.find( Warehouse.class, 1 );
			assertFalse( Hibernate.isPropertyInitialized( warehouse, "code" ) );
			warehouse.labels.add( "member" );
		} );
		scope.inSession( session -> assertEquals( "OWNER",
				session.createNativeQuery( "select owner_code from warehouse_labels", String.class ).getSingleResult() ) );
	}

	@Test
	void flushUntouchedCollectionKeepsOwnerKeyUnfetched(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var warehouse = new Warehouse();
			warehouse.id = 2;
			warehouse.code = "UNTOUCHED";
			session.persist( warehouse );
		} );
		scope.inTransaction( session -> {
			final var warehouse = session.find( Warehouse.class, 2 );
			assertFalse( Hibernate.isPropertyInitialized( warehouse, "code" ) );
			final var inspector = scope.getCollectingStatementInspector();
			inspector.clear();
			session.flush();
			inspector.assertExecutedCount( 0 );
			assertFalse( Hibernate.isPropertyInitialized( warehouse, "code" ) );
		} );
	}

	@Entity(name = "Warehouse")
	static class Warehouse {
		@Id int id;
		@Basic(fetch = FetchType.LAZY) @Column(unique = true) String code;
		@ElementCollection
		@CollectionTable(name = "warehouse_labels", joinColumns = @JoinColumn(name = "owner_code", referencedColumnName = "code"))
		Set<String> labels = new HashSet<>();
	}
}
