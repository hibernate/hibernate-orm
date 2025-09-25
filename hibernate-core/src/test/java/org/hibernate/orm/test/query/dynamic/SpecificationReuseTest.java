/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.dynamic;

import java.util.function.Consumer;

import org.hibernate.query.Order;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.specification.SelectionSpecification;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		BasicEntity.class,
		OtherEntity.class,
})
@SessionFactory(useCollectingStatementInspector = true)
@Jira( "https://hibernate.atlassian.net/browse/HHH-19781" )
public class SpecificationReuseTest {
	@Test
	public void dynamicSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var spec = SelectionSpecification.create( BasicEntity.class )
					.sort( Order.asc( BasicEntity_.position ) )
					.restrict( Restriction.like( BasicEntity_.name, "entity_%" ) );
			nTimes( spec, 3, s -> {
				inspector.clear();
				assertThat( s.createQuery( session ).list() ).extracting( BasicEntity::getId ).containsExactly( 2, 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "position", 2 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "name", 2 );
			} );
		} );
	}

	@Test
	public void hqlSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var spec = SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.sort( Order.asc( BasicEntity_.position ) )
					.restrict( Restriction.like( BasicEntity_.name, "entity_%" ) );
			nTimes( spec, 3, s -> {
				inspector.clear();
				assertThat( s.createQuery( session ).list() ).extracting( BasicEntity::getId ).containsExactly( 2, 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "position", 2 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "name", 2 );
			} );
		} );
	}

	@Test
	public void criteriaSelect(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var query = cb.createQuery( BasicEntity.class );
			final var root = query.from( BasicEntity.class );
			final var spec = SelectionSpecification.create( query.select( root ) )
					.sort( Order.asc( BasicEntity_.position ) )
					.restrict( Restriction.like( BasicEntity_.name, "entity_%" ) );
			nTimes( spec, 3, s -> {
				inspector.clear();
				assertThat( s.createQuery( session ).list() ).extracting( BasicEntity::getId ).containsExactly( 2, 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "position", 2 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "name", 2 );
			} );
		} );
	}

	@Test
	public void hqlMutation(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var e = new BasicEntity();
			e.setId( 33 );
			session.persist( e );
		} );
		scope.inTransaction( session -> {
			final var spec = MutationSpecification.create( BasicEntity.class, "update BasicEntity set name = 'entity_33'" )
					.restrict( Restriction.equal( BasicEntity_.id, 33 ) );
			nTimes( spec, 3, s -> {
				inspector.clear();
				assertThat( s.createQuery( session ).executeUpdate() ).isEqualTo( 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "id", 1 );
			} );
		} );
		scope.inTransaction( session -> {
			final var spec = MutationSpecification.create( BasicEntity.class, "delete BasicEntity" )
					.restrict( Restriction.equal( BasicEntity_.name, "entity_33" ) );
			nTimes( spec, 3, s -> {
				inspector.clear();
				s.createQuery(  session ).executeUpdate();
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "name", 1 );
			} );
		} );
	}

	@Test
	public void criteriaMutation(SessionFactoryScope scope) {
		final var inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final var e = new BasicEntity();
			e.setId( 44 );
			session.persist( e );
		} );
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var cu = cb.createCriteriaUpdate( BasicEntity.class );
			final var spec = MutationSpecification.create( cu.set( BasicEntity_.name, "entity_44" ) )
					.restrict( Restriction.equal( BasicEntity_.id, 44 ) );
			nTimes( spec, 3, s -> {
				inspector.clear();
				assertThat( s.createQuery( session ).executeUpdate() ).isEqualTo( 1 );
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "id", 1 );
			} );
		} );
		scope.inTransaction( session -> {
			final var cb = session.getCriteriaBuilder();
			final var cd = cb.createCriteriaDelete( BasicEntity.class );
			final var spec = MutationSpecification.create( cd )
					.restrict( Restriction.equal( BasicEntity_.name, "entity_44" ) );
			nTimes( spec, 3, s -> {
				inspector.clear();
				s.createQuery( session ).executeUpdate();
				inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "name", 1 );
			} );
		} );
	}

	<T> void nTimes(SelectionSpecification<T> spec, int n, Consumer<SelectionSpecification<T>> consumer) {
		for ( int i = 0; i < n; i++ ) {
			consumer.accept( spec );
		}
	}

	<T> void nTimes(MutationSpecification<T> spec, int n, Consumer<MutationSpecification<T>> consumer) {
		for ( int i = 0; i < n; i++ ) {
			consumer.accept( spec );
		}
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var e1 = new BasicEntity();
			e1.setId( 1 );
			e1.setName( "entity_1" );
			e1.setPosition( 99 );
			session.persist( e1 );

			final var e2 = new BasicEntity();
			e2.setId( 2 );
			e2.setName( "entity_2" );
			e2.setPosition( 42 );
			session.persist( e2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
