/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator.joined;

import jakarta.persistence.Tuple;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/mapping/inheritance/discriminator/joined/JoinedSubclassInheritance.hbm.xml"
)
@SessionFactory
public class DiscriminatorQueryUsageTests {
	@Test
	public void testUsageAsSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Tuple resultTuple = session
					.createQuery( "select p.id as id, type(p) as type from ParentEntity p", Tuple.class )
					.uniqueResult();

			Assertions.assertThat( resultTuple.get( "id" ) ).isEqualTo( 1 );
			Assertions.assertThat( resultTuple.get( "type" ) ).isEqualTo( ChildEntity.class );
		} );
	}

	@Test
	public void testUsageAsPredicate(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Integer id = session.createQuery( "select p.id from ParentEntity p where type(p) = ChildEntity", Integer.class ).uniqueResult();
			Assertions.assertThat( id ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testUsageAsPredicateOfUnderlyingType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Integer id = session.createQuery( "select p.id from ParentEntity p where type(p) = 'ce'", Integer.class ).uniqueResult();
			Assertions.assertThat( id ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testUsageAsPredicateWithParam(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Integer id = session.createQuery( "select p.id from ParentEntity p where type(p) = :type", Integer.class )
					.setParameter( "type", ChildEntity.class )
					.uniqueResult();
			Assertions.assertThat( id ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testUsageAsPredicateWithParamOfUnderlyingType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Query<Integer> query = session.createQuery(
					"select p.id from ParentEntity p where type(p) = :type",
					Integer.class
			);
			try {
				query.setParameter( "type", "ce" );
				fail( "Expected that setting the underlying type for a parameter of type Class<?> to fail!" );
			}
			catch (IllegalArgumentException ex) {
				// We expect this to fail
			}
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> session.persist( new ChildEntity( 1, "Child" ) ) );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
