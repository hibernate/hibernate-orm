package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Treating a standalone entity as its own type must retain its rows.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = TreatStandaloneEntityTest.Standalone.class)
@SessionFactory
@JiraKey("HHH-11062")
class TreatStandaloneEntityTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testTreatRootAsSameType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			for ( long id = 1; id <= 2; id++ ) {
				var entity = new Standalone();
				entity.id = id;
				session.persist( entity );
			}
		} );
		scope.inTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var query = builder.createQuery( Standalone.class );
			var root = query.from( Standalone.class );
			query.select( builder.treat( root, Standalone.class ) );
			assertThat( session.createQuery( query ).getResultList() ).extracting( entity -> entity.id )
					.containsExactlyInAnyOrder( 1L, 2L );
		} );
	}

	@Entity(name = "TreatStandalone")
	public static class Standalone {
		@Id
		Long id;
	}
}
