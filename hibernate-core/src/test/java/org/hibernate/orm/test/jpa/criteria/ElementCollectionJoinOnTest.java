package org.hibernate.orm.test.jpa.criteria;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.JoinType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Regression for parameterized predicates on outer joins to element collections.
///
/// @author Steve Ebersole
@DomainModel( annotatedClasses = { ElementCollectionJoinOnTest.Owner.class, ElementCollectionJoinOnTest.Detail.class } )
@SessionFactory
@JiraKey( "HHH-13709" )
class ElementCollectionJoinOnTest {

	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Owner matching = new Owner( 1 );
			matching.labels.addAll( Set.of( "match", "other" ) );
			matching.details.add( new Detail( "match" ) );
			matching.details.add( new Detail( "other" ) );
			Owner nonmatching = new Owner( 2 );
			nonmatching.labels.add( "other" );
			nonmatching.details.add( new Detail( "other" ) );
			session.persist( matching );
			session.persist( nonmatching );
			session.persist( new Owner( 3 ) );
		} );
	}

	@Test
	void basicElements(SessionFactoryScope scope) {
		assertOuterJoin( scope, false );
	}

	@Test
	void embeddableElements(SessionFactoryScope scope) {
		assertOuterJoin( scope, true );
	}

	private void assertOuterJoin(SessionFactoryScope scope, boolean embeddable) {
		scope.inTransaction( session -> {
			var builder = session.getCriteriaBuilder();
			var criteria = builder.createTupleQuery();
			var owner = criteria.from( Owner.class );
			var elements = owner.joinSet( embeddable ? "details" : "labels", JoinType.LEFT );
			var value = embeddable ? elements.<String>get( "text" ) : elements;
			var parameter = builder.parameter( String.class, "text" );
			elements.on( builder.equal( value, parameter ) );
			criteria.select( builder.tuple( owner.get( "id" ), value ) );
			criteria.orderBy( builder.asc( owner.get( "id" ) ) );

			var rows = session.createQuery( criteria ).setParameter( parameter, "match" ).getResultList();
			assertThat( rows ).hasSize( 3 );
			assertThat( rows ).extracting( row -> row.get( 0, Integer.class ) ).containsExactly( 1, 2, 3 );
			assertThat( rows ).extracting( row -> row.get( 1 ) ).containsExactly( "match", null, null );
		} );
	}

	@Entity( name = "CollectionJoinOwner" )
	public static class Owner {
		@Id
		Integer id;
		@ElementCollection
		Set<String> labels = new HashSet<>();
		@ElementCollection
		Set<Detail> details = new HashSet<>();

		public Owner() {
		}

		Owner(Integer id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class Detail {
		String text;

		public Detail() {
		}

		Detail(String text) {
			this.text = text;
		}
	}
}
