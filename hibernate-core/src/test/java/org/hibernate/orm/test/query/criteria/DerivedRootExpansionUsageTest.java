/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(
		annotatedClasses = {
				DerivedRootExpansionUsageTest.TestEntity.class,
		}
)
public class DerivedRootExpansionUsageTest {

	@Test
	@JiraKey("HHH-15435")
	public void test(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

					final CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
					final Root<TestEntity> from1 = query.from( TestEntity.class );
					query.where( from1.get( "name" ).isNotNull() );

					final SqmSelectStatement<Long> countQuery = (SqmSelectStatement<Long>) cb.createQuery( Long.class );
					final Subquery<Tuple> subQuery = countQuery.subquery( Tuple.class );

					final SqmSubQuery<Tuple> sqmSubQuery = (SqmSubQuery<Tuple>) subQuery;
					final SqmSelectStatement<Tuple> sqmOriginalQuery = (SqmSelectStatement<Tuple>) query;
					final SqmQuerySpec<Tuple> sqmOriginalQuerySpec = sqmOriginalQuery.getQuerySpec();
					final SqmQuerySpec<Tuple> sqmSubQuerySpec = sqmOriginalQuerySpec.copy( SqmCopyContext.simpleContext() );

					sqmSubQuery.setQueryPart(sqmSubQuerySpec);
					Root<TestEntity> subQuerySelectRoot = subQuery.from(TestEntity.class);
					sqmSubQuery.multiselect(subQuerySelectRoot.get("id").alias("id"));

					var root = countQuery.from(sqmSubQuery);
					countQuery.select(cb.count(root));

					try {
						entityManager.createQuery( countQuery ).getSingleResult();
						Assertions.fail( "Should fail because of `select(cb.count(root))`" );
					}
					catch (IllegalArgumentException ex) {
						assertThat( ex.getCause() ).isInstanceOf( SemanticException.class );
						assertThat( ex.getCause().getMessage() ).contains( "derivedRoot.get(\"alias1\")` or `derivedRoot.alias1`" );
					}
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {

		@Id
		private Integer id;

		private String name;

	}

}
