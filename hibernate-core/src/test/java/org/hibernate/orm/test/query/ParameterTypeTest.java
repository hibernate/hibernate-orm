/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import org.hibernate.query.sqm.tree.spi.expression.SqmParameter;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(annotatedClasses = ParameterTypeTest.Item.class)
@SessionFactory
class ParameterTypeTest {
	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ParameterTypeItem" ).executeUpdate();
			final var item = new Item();
			item.id = 1;
			item.name = "item";
			item.address = new Address();
			item.address.city = "Paris";
			session.persist( item );
		} );
	}

	@Test
	void criteriaDeclaredTypes(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		for ( var type : List.of( String.class, Item.class, Address.class, List.class, Iterable.class, Unmapped.class ) ) {
			final var named = builder.parameter( type, "p" );
			final var unnamed = builder.parameter( type );
			assertSame( type, named.getParameterType() );
			assertSame( type, unnamed.getParameterType() );
			assertSame( type, named.getJavaType() );
			assertSame( type, ((SqmParameter<?>) named).copy().getParameterType() );
		}
	}

	@Test
	void criteriaEntityParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Item.class );
			final var root = criteria.from( Item.class );
			final var parameter = builder.parameter( Item.class, "item" );
			criteria.select( root ).where( builder.equal( root, parameter ) );
			final var query = session.createQuery( criteria );
			assertSame( Item.class, parameter.getParameterType() );
			assertSame( Item.class, query.getParameter( "item" ).getParameterType() );
			assertSame( Item.class, query.getParameters().iterator().next().getParameterType() );
			final var item = session.find( Item.class, 1 );
			query.setParameter( parameter, item );
			assertSame( item, query.getSingleResult() );
		} );
	}

	@Test
	void criteriaDeclaredTypeWithoutInference(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Item.class );
			final var root = criteria.from( Item.class );
			final var parameter = builder.parameter( Item.class, "p" );
			criteria.select( root ).where( parameter.isNull() );
			final var query = session.createQuery( criteria );
			assertSame( Item.class, query.getParameter( "p" ).getParameterType() );
			assertThrows( IllegalArgumentException.class, () -> query.setParameter( "p", null, String.class ) );
		} );
	}

	@Test
	void criteriaEmbeddableParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Item.class );
			final var root = criteria.from( Item.class );
			final var parameter = builder.parameter( Address.class );
			criteria.select( root ).where( builder.equal( root.get( "address" ), parameter ) );
			final var query = session.createQuery( criteria );
			assertSame( Address.class, query.getParameters().iterator().next().getParameterType() );
			final var item = session.find( Item.class, 1 );
			query.setParameter( parameter, item.address );
			assertSame( item, query.getSingleResult() );
		} );
	}

	@Test
	void criteriaCollectionParameter(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Item.class );
			final var root = criteria.from( Item.class );
			final var parameter = builder.parameter( List.class, "names" );
			criteria.select( root ).where( root.get( "name" ).in( parameter ) );
			final var query = session.createQuery( criteria );
			assertSame( List.class, parameter.getParameterType() );
			assertSame( List.class, query.getParameter( "names" ).getParameterType() );
			query.setParameter( parameter, List.of( "item" ) );
			assertEquals( 1, query.getResultList().size() );
			query.setParameterList( "names", List.of( "item" ), String.class );
			assertEquals( 1, query.getResultList().size() );
		} );
	}

	@Test
	void registeredProcedureParameterTypes(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var named = session.createStoredProcedureQuery( "unused" );
			named.registerStoredProcedureParameter( "p", Integer.class, ParameterMode.IN );
			assertSame( Integer.class, named.getParameter( "p" ).getParameterType() );
			final var positional = session.createStoredProcedureQuery( "unused" );
			positional.registerStoredProcedureParameter( 1, String.class, ParameterMode.IN );
			assertSame( String.class, positional.getParameter( 1 ).getParameterType() );
		} );
	}

	@Test
	void inferredHqlType(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createQuery( "from ParameterTypeItem where name = :p", Item.class );
			assertSame( String.class, query.getParameter( "p" ).getParameterType() );
			query.setParameter( "p", null );
			assertSame( String.class, query.getParameter( "p" ).getParameterType() );
			assertThrows( IllegalArgumentException.class, () -> query.setParameter( "p", 1, Integer.class ) );
		} );
	}

	@Test
	void unknownHqlType(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createQuery( "from ParameterTypeItem where :p is null", Item.class );
			final var parameter = query.getParameter( "p" );
			assertThrows( IllegalStateException.class, parameter::getParameterType );
			query.setParameter( "p", null );
			assertThrows( IllegalStateException.class, parameter::getParameterType );
			assertEquals( 1, query.getResultList().size() );
		} );
	}

	@Test
	void hqlTypeFromBinding(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createQuery( "from ParameterTypeItem where :p is null", Item.class );
			final var parameter = query.getParameter( "p" );
			query.setParameter( "p", "value" );
			assertSame( String.class, parameter.getParameterType() );
			assertTrue( query.getResultList().isEmpty() );
		} );
	}

	@Test
	void nativeNamedTypeFromBinding(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createNativeQuery( "select id from ParameterTypeItem where name = :p", Integer.class );
			final var parameter = query.getParameter( "p" );
			assertThrows( IllegalStateException.class, parameter::getParameterType );
			assertFalse( query.isBound( parameter ) );
			assertThrows( IllegalStateException.class, () -> query.getParameter( "p", String.class ) );
			query.setParameter( "p", "item", String.class );
			assertSame( String.class, parameter.getParameterType() );
			assertSame( parameter, query.getParameter( "p", String.class ) );
			assertSame( parameter, query.getParameters().iterator().next() );
			assertTrue( query.isBound( parameter ) );
			assertEquals( "item", query.getParameterValue( parameter ) );
			assertEquals( 1, query.getSingleResult() );
			query.setParameter( query.getParameter( "p", String.class ), "other" );
			assertTrue( query.getResultList().isEmpty() );
			assertThrows( IllegalArgumentException.class, () -> query.getParameter( "p", Integer.class ) );
		} );
	}

	@Test
	void nativePositionalTypeFromBinding(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createNativeQuery( "select id from ParameterTypeItem where name = ?1", Integer.class );
			final var parameter = query.getParameter( 1 );
			assertThrows( IllegalStateException.class, parameter::getParameterType );
			assertThrows( IllegalStateException.class, () -> query.getParameter( 1, String.class ) );
			query.setParameter( 1, "item", String.class );
			assertSame( String.class, parameter.getParameterType() );
			assertSame( parameter, query.getParameter( 1, String.class ) );
			assertEquals( 1, query.getSingleResult() );
		} );
	}

	@Test
	void nativeNullBinding(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var query = session.createNativeQuery( "select id from ParameterTypeItem where name = :p", Integer.class );
			final var parameter = query.getParameter( "p" );
			query.setParameter( "p", null );
			assertThrows( IllegalStateException.class, parameter::getParameterType );
			assertTrue( query.getResultList().isEmpty() );
			query.setParameter( "p", "item" );
			assertSame( String.class, parameter.getParameterType() );
			assertEquals( 1, query.getSingleResult() );
			query.setParameter( "p", null, String.class );
			assertSame( String.class, parameter.getParameterType() );
			assertTrue( query.getResultList().isEmpty() );
		} );
	}

	@Test
	void nativeBindingTypesAreNotShared(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String sql = "select id from ParameterTypeItem where :p is null";
			final var first = session.createNativeQuery( sql, Integer.class );
			final var second = session.createNativeQuery( sql, Integer.class );
			first.setParameter( "p", null, String.class );
			assertThrows( IllegalStateException.class, () -> second.getParameter( "p" ).getParameterType() );
			second.setParameter( "p", null, Integer.class );
			assertSame( String.class, first.getParameter( "p" ).getParameterType() );
			assertSame( Integer.class, second.getParameter( "p" ).getParameterType() );
			assertEquals( 1, first.getSingleResult() );
			assertEquals( 1, second.getSingleResult() );
			assertThrows( IllegalStateException.class,
					() -> session.createNativeQuery( sql, Integer.class ).getParameter( "p" ).getParameterType() );
		} );
	}

	@Test
	void hqlBindingTypesAreNotShared(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final String hql = "from ParameterTypeItem where :p is null";
			final var first = session.createQuery( hql, Item.class );
			final var second = session.createQuery( hql, Item.class );
			first.setParameter( "p", null, String.class );
			assertThrows( IllegalStateException.class, () -> second.getParameter( "p" ).getParameterType() );
			second.setParameter( "p", null, Integer.class );
			assertSame( String.class, first.getParameter( "p" ).getParameterType() );
			assertSame( Integer.class, second.getParameter( "p" ).getParameterType() );
			assertEquals( 1, first.getResultList().size() );
			assertEquals( 1, second.getResultList().size() );
		} );
	}

	@Entity(name = "ParameterTypeItem")
	static class Item {
		@Id
		Integer id;
		String name;
		@Embedded
		Address address;
	}

	@Embeddable
	static class Address {
		String city;
	}

	static class Unmapped {
	}
}
