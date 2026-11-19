package org.hibernate.orm.test.generator.batch;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Updating an entity with a filtered (hidden) to-one association and a batch-capable pre-update
 * generator ({@code @UpdateTimestamp}) must bind the generated value.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		BatchGenerationFilteredUpdateTest.Owner.class,
		BatchGenerationFilteredUpdateTest.Target.class,
})
class BatchGenerationFilteredUpdateTest {

	@Test
	void updateTimestampWithFilteredAssociation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Target t = new Target();
			t.id = 1L;
			t.active = true;
			session.persist( t );
			final Owner o = new Owner();
			o.id = 1L;
			o.name = "before";
			o.target = t;
			session.persist( o );
		} );
		scope.inTransaction( session -> session.createNativeMutationQuery(
				"update FilteredUpdateTarget set active = false" ).executeUpdate() );
		final Instant[] before = new Instant[1];
		scope.inTransaction( session -> before[0] = session.find( Owner.class, 1L ).updated );
		try {
			Thread.sleep( 50 );
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
		scope.inTransaction( session -> {
			final Owner o = session.find( Owner.class, 1L );
			assertThat( o.target ).as( "target is hidden by the restriction" ).isNull();
			assertThat( FilteredAssociationState.hasFilteredAssociations(
					session.unwrap( SessionImplementor.class ).getPersistenceContextInternal().getEntry( o ) ) )
					.as( "filtered association state must be active" ).isTrue();
			o.name = "after";
		} );
		scope.inTransaction( session -> {
			final Owner o = session.find( Owner.class, 1L );
			assertThat( o.name ).isEqualTo( "after" );
			assertThat( o.updated ).isNotNull().isAfter( before[0] );
		} );
	}

	@Entity(name = "FilteredUpdateOwner")
	static class Owner {
		@Id
		Long id;
		String name;
		@UpdateTimestamp
		Instant updated;
		@ManyToOne
		@JoinColumn(name = "target_id")
		Target target;
	}

	@Entity(name = "FilteredUpdateTarget")
	@SQLRestriction("active = true")
	static class Target {
		@Id
		Long id;
		boolean active;
	}
}
