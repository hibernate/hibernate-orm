/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance;

import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaCteCriteria;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.hql.spi.SqmQueryImplementor;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa(
		annotatedClasses = CriteriaRecursiveCteCopyTest.Account.class,
		properties = @Setting( name = AvailableSettings.CRITERIA_COPY_TREE, value = "true" )
)
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsRecursiveCtes.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16483" )
public class CriteriaRecursiveCteCopyTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final Account parent = new Account( "Parent Account", null );
			final Account child = new Account( "Child Account", parent );
			entityManager.persist( parent );
			entityManager.persist( child );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from Account a where a.parent is not null" ).executeUpdate();
			entityManager.createQuery( "delete from Account" ).executeUpdate();
		} );
	}

	@Test
	public void testRecursiveCte(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final HibernateCriteriaBuilder builder = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();
			final JpaCriteriaQuery<Tuple> query = builder.createTupleQuery();
			final JpaCriteriaQuery<Tuple> nonRecurQuery = builder.createTupleQuery();
			final JpaRoot<Account> nonRecurRoot = nonRecurQuery.from( Account.class );
			nonRecurQuery.multiselect(
					nonRecurRoot.get( "id" ).alias( "id" ),
					nonRecurRoot.get( "name" ).alias( "name" ),
					nonRecurRoot.get( "parent" ).get( "id" ).alias( "parent_id" )
			);
			nonRecurQuery.where( builder.like( builder.lower( nonRecurRoot.get( "name" ) ), "%child%" ) );
			final JpaCteCriteria<Tuple> accountChainTable = query.withRecursiveUnionAll( nonRecurQuery, (cte) -> {
				final JpaCriteriaQuery<Tuple> innerQuery = builder.createTupleQuery();
				final JpaRoot<Account> accountRoot = innerQuery.from( Account.class );
				final JpaRoot<Tuple> cteRoot = innerQuery.from( cte );
				innerQuery.multiselect(
						accountRoot.get( "id" ),
						accountRoot.get( "name" ),
						accountRoot.get( "parent" ).get( "id" )
				);
				innerQuery.where( builder.equal( accountRoot.get( "id" ), cteRoot.get( "parent_id" ) ) );
				return innerQuery;
			} );
			final JpaRoot<Tuple> root = query.from( accountChainTable );
			query.multiselect( root.get( "id" ), root.get( "name" ), root.get( "parent_id" ) );
			final SqmQueryImplementor<Tuple> querySqm = (SqmQueryImplementor<Tuple>) entityManager.createQuery( query );
			assertThat( querySqm.getSqmStatement() )
					.withFailMessage( "Query statement should have been copied: [%s]", querySqm.getSqmStatement() )
					.isNotSameAs( query );
			final List<Tuple> resultList = querySqm.getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.get( 0 ).get( 1 ) ).isEqualTo( "Child Account" );
			assertThat( resultList.get( 1 ).get( 1 ) ).isEqualTo( "Parent Account" );
		} );
	}


	@Entity( name = "Account" )
	public static class Account {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToOne
		private Account parent;

		public Account() {
		}

		public Account(String name, Account parent) {
			this.name = name;
			this.parent = parent;
		}
	}
}
