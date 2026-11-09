package org.hibernate.orm.test.query.sql;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel
@SessionFactory
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-21096")
class NativeQueryPlanCacheTest {

	// Use a different SQL comment for each test to keep their cached plans independent.
	@Test
	void differentNamedParameters(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* named */ cast(:a as integer)" )
					.setParameter( "a", 7 ).getResultList() )
					.singleElement().isEqualTo( 7 );
		} );
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* named */ cast(:b as integer)" )
					.setParameter( "b", 8 ).getResultList() )
					.singleElement().isEqualTo( 8 );
		} );
	}

	@Test
	void ordinalThenNamedParameter(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* ordinal */ cast(?1 as integer)" )
					.setParameter( 1, 7 ).getResultList() )
					.singleElement().isEqualTo( 7 );
		} );
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* ordinal */ cast(:x as integer)" )
					.setParameter( "x", 8 ).getResultList() )
					.singleElement().isEqualTo( 8 );
		} );
	}

	@Test
	void sameNamedParametersInDifferentOrder(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* reordered */ cast(:a as integer) * 10 + cast(:b as integer)" )
					.setParameter( "a", 1 ).setParameter( "b", 2 ).getResultList() )
					.singleElement().isEqualTo( 12 );
		} );
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* reordered */ cast(:b as integer) * 10 + cast(:a as integer)" )
					.setParameter( "a", 1 ).setParameter( "b", 2 ).getResultList() )
					.singleElement().isEqualTo( 21 );
		} );
	}

	@Test
	void differentNamedParametersWithResultClass(SessionFactoryScope scope) {
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* typed */ cast(:a as integer)", Object.class )
					.setParameter( "a", 7 ).getResultList() )
					.containsExactly( 7 );
		} );
		scope.inSession( session -> {
			assertThat( session.createNativeQuery( "select /* typed */ cast(:b as integer)", Object.class )
					.setParameter( "b", 8 ).getResultList() )
					.containsExactly( 8 );
		} );
	}
}
