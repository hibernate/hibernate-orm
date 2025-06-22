/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.dynamic;

import jakarta.persistence.criteria.CommonAbstractCriteria;
import jakarta.persistence.criteria.CriteriaQuery;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionLazyDelegator;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.criteria.CriteriaDefinition;
import org.hibernate.query.specification.MutationSpecification;
import org.hibernate.query.specification.SelectionSpecification;
import org.hibernate.query.range.Range;
import org.hibernate.query.restriction.Path;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {BasicEntity.class, OtherEntity.class})
@org.hibernate.testing.orm.junit.SessionFactory(useCollectingStatementInspector = true)
public class SimpleQuerySpecificationTests {
	@Test
	void testSimpleSelectionOrder(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.sort( Order.asc( BasicEntity_.position ) )
					.createQuery( session )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
	}

	@Test
	void testSimpleSelectionOrderMultiple(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.sort( Order.asc( BasicEntity_.position ) )
					.sort( Order.asc( BasicEntity_.id ) )
					.createQuery( session )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position,be1_0.id", " order by be1_0.\"position\",be1_0.id" );
	}

	@Test
	void testSimpleSelectionSetOrdering(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.resort( Order.asc( BasicEntity_.position ) )
					.createQuery( session )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
	}

	@Test
	void testSimpleSelectionSetOrderingMultiple(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.resort( List.of( Order.asc( BasicEntity_.position ), Order.asc( BasicEntity_.id ) ) )
					.createQuery( session )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position,be1_0.id", " order by be1_0.\"position\",be1_0.id" );
	}

	@Test
	void testSimpleSelectionSetOrderingReplace(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.resort( Order.asc( BasicEntity_.id ) )
					.resort( Order.asc( BasicEntity_.position ) )
					.createQuery( session )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.sort( Order.asc( BasicEntity_.id ) )
					.resort( Order.asc( BasicEntity_.position ) )
					.createQuery( session )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
	}

	@Test
	void testSimpleSelectionRestriction(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.restrict( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
					.createQuery( session )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " where be1_0.position between ? and ?", " where be1_0.\"position\" between ? and ?" );
	}

	@Test
	void testSimpleMutationRestriction(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			MutationSpecification.create( BasicEntity.class, "delete BasicEntity" )
					.restrict( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
					.createQuery( session )
					.executeUpdate();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " where be1_0.position between ? and ?", " where be1_0.\"position\" between ? and ?" );
	}

	@Test
	@JiraKey("HHH-19531")
	void testSelectionOnSessionProxy(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			var sessionProxy = new SessionLazyDelegator( () -> session );
			// The test only makes sense if this is true. It currently is, but who knows what the future has in store for us.
			//noinspection ConstantValue
			assert !(sessionProxy instanceof SharedSessionContractImplementor);

			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity" )
					.createQuery( sessionProxy )
					.list();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
	}

	@Test
	@JiraKey("HHH-19531")
	void testMutationOnSessionProxy(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			var sessionProxy = new SessionLazyDelegator( () -> session );
			// The test only makes sense if this is true. It currently is, but who knows what the future has in store for us.
			//noinspection ConstantValue
			assert !(sessionProxy instanceof SharedSessionContractImplementor);

			sqlCollector.clear();
			MutationSpecification.create( BasicEntity.class, "delete BasicEntity" )
					.createQuery( sessionProxy )
					.executeUpdate();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
	}

	@Test
	void testSimpleMutationRestrictionAsReference(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		var deleteBasicEntity = MutationSpecification
				.create( BasicEntity.class, "delete BasicEntity" )
				.restrict( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
				.reference();
		factoryScope.inTransaction( session -> {
			sqlCollector.clear();
			session.createQuery( deleteBasicEntity ).executeUpdate();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " where be1_0.position between ? and ?", " where be1_0.\"position\" between ? and ?" );
	}

	@Test
	void testSimpleMutationRestrictionStatelessAsReference(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();
		var deleteBasicEntity = MutationSpecification
				.create( BasicEntity.class, "delete BasicEntity" )
				.restrict( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
				.reference();
		factoryScope.inStatelessTransaction( statelessSession -> {
			sqlCollector.clear();
			statelessSession.createQuery( deleteBasicEntity ).executeUpdate();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " where be1_0.position between ? and ?", " where be1_0.\"position\" between ? and ?" );
	}

	@Test
	void testRootEntityForm(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class )
					.sort( Order.asc( BasicEntity_.position ) )
					.createQuery( session )
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
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
			SelectionSpecification.create( query )
					.sort( Order.asc( BasicEntity_.position ) )
					.fetch( Path.from(BasicEntity.class).to( BasicEntity_.other ) )
					.createQuery( session )
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
	}

	@Test
	void testAugmentation(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class )
					.augment( (builder, query, entity) -> query.where( builder.like( entity.get( BasicEntity_.name ), "%" ) ) )
					.sort( Order.asc( BasicEntity_.position ) )
					.createQuery( session )
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
	}

	@Test
	void testAugmentationViaCriteriaDefinition(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class )
					.augment( (builder, query, entity) ->
							new CriteriaDefinition<>( query ) {{
								where( like( entity.get( BasicEntity_.name ), "%" ),
										equal( entity.get( BasicEntity_.position), 1 ) );
							}}
					)
					.sort( Order.asc( BasicEntity_.position ) )
					.createQuery( session )
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
	}
	@Test
	void testBaseParameters(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			SelectionSpecification.create( BasicEntity.class, "from BasicEntity where id > :id" )
					.restrict( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
					.createQuery( session )
					.setParameter( "id", 200 )
					.getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) ).containsAnyOf(
				" where be1_0.id>? and be1_0.position between ? and ?",
				" where be1_0.id>? and be1_0.\"position\" between ? and ?" );
	}

	@Test
	void testIllegalSelection(SessionFactoryScope factoryScope) {
		final SessionFactory factory = factoryScope.getSessionFactory();
		try {
			SelectionSpecification.create( BasicEntity.class, "delete BasicEntity" )
					.validate( factory.getCriteriaBuilder() );
			fail( "Expecting a IllegalSelectQueryException, but not thrown" );
		}
		catch (IllegalSelectQueryException expected) {
		}
	}

	@Test
	void testIllegalMutation(SessionFactoryScope factoryScope) {
		final SessionFactory factory = factoryScope.getSessionFactory();
		try {
			MutationSpecification.create( BasicEntity.class, "from BasicEntity" )
					.validate( factory.getCriteriaBuilder() );
			fail( "Expecting a IllegalMutationQueryException, but not thrown" );
		}
		catch (IllegalMutationQueryException expected) {
		}
	}

	@Test
	void testBuildCriteriaDelete(SessionFactoryScope factoryScope) {
		final SessionFactory factory = factoryScope.getSessionFactory();
		CommonAbstractCriteria deleteBasicEntity =
				MutationSpecification.create( BasicEntity.class,
								"delete BasicEntity" )
						.validate( factory.getCriteriaBuilder() )
						.buildCriteria( factory.getCriteriaBuilder() );
	}

	@Test
	void testBuildCriteriaQuery(SessionFactoryScope factoryScope) {
		final SessionFactory factory = factoryScope.getSessionFactory();
		CriteriaQuery<BasicEntity> query =
				SelectionSpecification.create( BasicEntity.class,
								"from BasicEntity" )
						.validate( factory.getCriteriaBuilder() )
						.buildCriteria( factory.getCriteriaBuilder() );
	}

	@Test
	void testUseAsTypedQueryRef(SessionFactoryScope factoryScope) {
		final SQLStatementInspector sqlCollector = factoryScope.getCollectingStatementInspector();

		factoryScope.inTransaction( (session) -> {
			sqlCollector.clear();
			var spec = SelectionSpecification.create( BasicEntity.class )
					.restrict( Restriction.restrict( BasicEntity_.position, Range.closed( 1, 5 ) ) )
					.sort( Order.asc( BasicEntity_.position ) );
			session.createQuery(spec.reference()).getResultList();
		} );

		assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
		assertThat( sqlCollector.getSqlQueries().get( 0 ) )
				.containsAnyOf( " order by be1_0.position", " order by be1_0.\"position\"" );
	}

}
