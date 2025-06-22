/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.set;

import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian Beikov
 */
@DomainModel( standardModels = StandardDomainModel.GAMBIT )
@ServiceRegistry
@SessionFactory
public class SetOperationJpaCriteriaTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new EntityOfLists( 1, "first" ) );
					session.persist( new EntityOfLists( 2, "second" ) );
					session.persist( new EntityOfLists( 3, "third" ) );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	public void testUnionAll(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					JpaCriteriaQuery<EntityOfLists> query1 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root1 = query1.from( EntityOfLists.class );
					query1.where( cb.equal( root1.get( "id" ), 1 ) );

					JpaCriteriaQuery<EntityOfLists> query2 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root2 = query2.from( EntityOfLists.class );
					query2.where( cb.equal( root2.get( "id" ), 2 ) );

					List<EntityOfLists> list = session.createQuery(
							cb.unionAll( query1, query2 )
					).list();
					assertThat( list.size(), is( 2 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	public void testUnionAllLimit(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					JpaCriteriaQuery<EntityOfLists> query1 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root1 = query1.from( EntityOfLists.class );
					query1.where( cb.equal( root1.get( "id" ), 1 ) );

					JpaCriteriaQuery<EntityOfLists> query2 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root2 = query2.from( EntityOfLists.class );
					query2.where( cb.equal( root2.get( "id" ), 2 ) );

					List<EntityOfLists> list = session.createQuery(
							cb.unionAll( query1, query2 )
								.orderBy( cb.asc( cb.literal( 1 ) ) )
								.fetch( 1 )
					).list();
					assertThat( list.size(), is( 1 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
	public void testUnionAllLimitSubquery(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					JpaCriteriaQuery<EntityOfLists> query1 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root1 = query1.from( EntityOfLists.class );
					query1.where( cb.equal( root1.get( "id" ), 1 ) );

					JpaCriteriaQuery<EntityOfLists> query2 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root2 = query2.from( EntityOfLists.class );
					query2.where( cb.equal( root2.get( "id" ), 2 ) );

					List<EntityOfLists> list = session.createQuery(
							cb.unionAll(
									query1,
									query2.orderBy( cb.asc( cb.literal( 1 ) ) )
											.fetch( 1 )
							)
					).list();
					assertThat( list.size(), is( 2 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
	public void testUnionAllLimitNested(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					JpaCriteriaQuery<EntityOfLists> query1 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root1 = query1.from( EntityOfLists.class );
					query1.where( cb.equal( root1.get( "id" ), 1 ) );

					JpaCriteriaQuery<EntityOfLists> query2 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root2 = query2.from( EntityOfLists.class );
					query2.where( cb.equal( root2.get( "id" ), 2 ) );

					List<EntityOfLists> list = session.createQuery(
							cb.unionAll(
									query1,
									query2.orderBy( cb.asc( cb.literal( 1 ) ) )
											.fetch( 1 )
							).orderBy( cb.asc( cb.literal( 1 ) ) )
									.fetch( 1 )
					).list();
					assertThat( list.size(), is( 1 ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUnion.class)
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsOrderByInSubquery.class)
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16754" )
	public void testUnionAllSubqueryOrderByPath(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					HibernateCriteriaBuilder cb = session.getCriteriaBuilder();

					JpaCriteriaQuery<EntityOfLists> query1 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root1 = query1.from( EntityOfLists.class );
					query1.where( cb.equal( root1.get( "id" ), 1 ) );

					JpaCriteriaQuery<EntityOfLists> query2 = cb.createQuery( EntityOfLists.class );
					JpaRoot<EntityOfLists> root2 = query2.from( EntityOfLists.class );
					query2.where( cb.equal( root2.get( "id" ), 2 ) );

					List<EntityOfLists> list = session.createQuery(
							cb.unionAll(
									query1,
									query2.orderBy( cb.asc( root2.get("name") ) )
											.fetch( 1 )
							)
					).list();
					assertThat( list.size(), is( 2 ) );
				}
		);
	}
}
