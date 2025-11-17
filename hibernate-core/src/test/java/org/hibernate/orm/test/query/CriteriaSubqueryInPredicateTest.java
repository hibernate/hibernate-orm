/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18502" )
public class CriteriaSubqueryInPredicateTest {
	@Test
	public void testInCollection(SessionFactoryScope scope) {
		executeQuery( scope, 1, sub -> sub.in( List.of( "entity_1", "another_entity" ) ) );
	}

	@Test
	public void testInArray(SessionFactoryScope scope) {
		executeQuery( scope, 2, sub -> sub.in( "entity_2", "another_entity" ) );
	}

	@Test
	public void testInLiteral(SessionFactoryScope scope) {
		executeQuery( scope, 3, sub -> sub.in( "entity_3" ) );
	}

	private void executeQuery(SessionFactoryScope scope, Integer expectedId, InPredicateProducer<String> producer) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<BasicEntity> cq = cb.createQuery( BasicEntity.class );
			final Root<BasicEntity> root = cq.from( BasicEntity.class );

			final Subquery<String> sub = cq.subquery( String.class );
			final Root<BasicEntity> subRoot = sub.from( BasicEntity.class );
			sub.select( subRoot.get( "data" ) ).where( cb.equal( subRoot.get( "id" ), root.get( "id" ) ) );

			cq.select( root ).where( producer.accept( sub ) );

			final List<BasicEntity> resultList = session.createQuery( cq ).getResultList();
			assertThat( resultList ).hasSize( 1 ).extracting( BasicEntity::getId ).containsOnlyOnce( expectedId );
		} );
	}

	private interface InPredicateProducer<T> {
		Predicate accept(Subquery<T> sub);
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "entity_1" ) );
			session.persist( new BasicEntity( 2, "entity_2" ) );
			session.persist( new BasicEntity( 3, "entity_3" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
