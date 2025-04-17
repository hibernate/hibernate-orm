/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.dynamic;

import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.range.Range;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = BasicEntity.class)
@SessionFactory(useCollectingStatementInspector = true)
public class SimpleQuerySpecificationTests {
	@Test
	void testSimpleSelectionOrder(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity", BasicEntity.class )
					.addOrdering( Order.asc( BasicEntity_.position ) )
					.createQuery()
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position" );
	}

	@Test
	void testSimpleSelectionOrderMultiple(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity", BasicEntity.class )
					.addOrdering( Order.asc( BasicEntity_.position ) )
					.addOrdering( Order.asc( BasicEntity_.id ) )
					.createQuery()
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position,be1_0.id" );
	}

	@Test
	void testSimpleSelectionSetOrdering(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity", BasicEntity.class )
					.setOrdering( Order.asc( BasicEntity_.position ) )
					.createQuery()
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position" );
	}

	@Test
	void testSimpleSelectionSetOrderingMultiple(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity", BasicEntity.class )
					.setOrdering( List.of( Order.asc( BasicEntity_.position ), Order.asc( BasicEntity_.id ) ) )
					.createQuery()
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position,be1_0.id" );
	}

	@Test
	void testSimpleSelectionSetOrderingReplace(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity", BasicEntity.class )
					.setOrdering( Order.asc( BasicEntity_.id ) )
					.setOrdering( Order.asc( BasicEntity_.position ) )
					.createQuery()
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position" );

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity", BasicEntity.class )
					.addOrdering( Order.asc( BasicEntity_.id ) )
					.setOrdering( Order.asc( BasicEntity_.position ) )
					.createQuery()
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position" );
	}

	@Test
	void testSimpleSelectionRestriction(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity", BasicEntity.class )
					.addRestriction( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
					.createQuery()
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " where be1_0.position between ? and ?" );
	}

	@Test
	void testSimpleMutationRestriction(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createMutationSpecification( "delete BasicEntity", BasicEntity.class )
					.addRestriction( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
					.createQuery()
					.executeUpdate();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " where be1_0.position between ? and ?" );
	}

	@Test
	void testRootEntityForm(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( BasicEntity.class )
					.addOrdering( Order.asc( BasicEntity_.position ) )
					.createQuery()
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position" );
	}

	@Test
	void testCriteriaForm(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			var criteriaBuilder = session.getCriteriaBuilder();
			var query = criteriaBuilder.createQuery( BasicEntity.class );
			var entity = query.from( BasicEntity.class );
			query.select( entity );
			query.where( criteriaBuilder.like( entity.get( BasicEntity_.name ), "%" ) );
			session.createSelectionSpecification( query )
					.addOrdering( Order.asc( BasicEntity_.position ) )
					.createQuery()
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " order by be1_0.position" );
	}

	@Test
	void testBaseParameters(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			session.createSelectionSpecification( "from BasicEntity where id > :id", BasicEntity.class )
					.addRestriction( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
					.createQuery()
					.setParameter( "id", 200 )
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " where be1_0.id>? and be1_0.position between ? and ?" );
	}

	@Test
	void testIllegalSelection(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createSelectionSpecification( "delete BasicEntity", BasicEntity.class );
				fail( "Expecting a IllegalSelectQueryException, but not thrown" );
			}
			catch (IllegalSelectQueryException expected) {
			}
		} );
	}

	@Test
	void testIllegalMutation(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			try {
				session.createMutationSpecification( "from BasicEntity", BasicEntity.class );
				fail( "Expecting a IllegalMutationQueryException, but not thrown" );
			}
			catch (IllegalMutationQueryException expected) {
			}
		} );
	}
}
