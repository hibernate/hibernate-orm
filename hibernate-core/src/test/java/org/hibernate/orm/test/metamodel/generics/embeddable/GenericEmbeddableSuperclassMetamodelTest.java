/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.metamodel.generics.embeddable;

import java.util.Date;

import org.hibernate.SessionFactory;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		Parent.class,
		AbstractValueObject.class,
		CreationDate.class,
		SomeNumber.class,
		SomeString.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18490" )
public class GenericEmbeddableSuperclassMetamodelTest {
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<Tuple> cq = cb.createTupleQuery();
			final Root<Parent> from = cq.from( Parent.class );

			final Path someStringPath = from.get( Parent_.someString ).get( SomeString_.value );
			final Path someNumberPath = from.get( Parent_.someNumber ).get( SomeNumber_.value );
			final Path timestampPath = from.get( Parent_.date ).get( CreationDate_.value );
			final Expression<Integer> maxNumber = cb.max( someNumberPath );
			final Expression<Date> maxTimestamp = cb.function( "max", Date.class, timestampPath );
			cq.select( cb.tuple( someStringPath, maxTimestamp, maxNumber ) );
			cq.groupBy( someStringPath );

			final TypedQuery<Tuple> query = entityManager.createQuery( cq );
			final Tuple result = query.getSingleResult();

			assertThat( result.get( 0, String.class ) ).isEqualTo( "something" );
			assertThat( result.get( 1, Date.class ) ).isNotNull();
			assertThat( result.get( 2, Object.class ) ).isEqualTo( 42 );
		} );
	}

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> entityManager.persist( new Parent(
				new SomeString( "something" ),
				new CreationDate( new Date() ),
				new SomeNumber( 42 )
		) ) );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().unwrap( SessionFactory.class ).getSchemaManager().truncateMappedObjects();
	}
}
