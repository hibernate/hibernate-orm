package org.hibernate.orm.test.softdelete.collections;

import java.util.HashSet;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.cfg.FlushSettings.FLUSH_QUEUE_TYPE;

@DomainModel(annotatedClasses = CollectionOwner2.class)
@ServiceRegistry(settings = @Setting(name = FLUSH_QUEUE_TYPE, value = "graph"))
@SessionFactory
public class SoftDeleteCollectionRemovalTest {
	@Test
	void removeOneElementThenClear(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var first = new CollectionOwner2( 1, "first" );
			first.setBatchLoadable( new HashSet<>( List.of( "a", "b", "c" ) ) );
			final var second = new CollectionOwner2( 2, "second" );
			second.setBatchLoadable( new HashSet<>( List.of( "a" ) ) );
			session.persist( first );
			session.persist( second );
		} );
		scope.inTransaction( session -> session.find( CollectionOwner2.class, 1 ).getBatchLoadable().remove( "a" ) );
		final String inactiveCount = "select count(*) from batch_loadables where owner_fk = 1 and active = 'N'";
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( inactiveCount, Long.class ).getSingleResult() ).isEqualTo( 1L );
			final var remaining = session.find( CollectionOwner2.class, 1 ).getBatchLoadable();
			assertThat( remaining ).containsExactlyInAnyOrder( "b", "c" );
			remaining.clear();
		} );
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( inactiveCount, Long.class ).getSingleResult() ).isEqualTo( 3L );
			assertThat( session.find( CollectionOwner2.class, 2 ).getBatchLoadable() ).containsExactly( "a" );
		} );
	}
}
