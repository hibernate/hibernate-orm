/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.Expression;

import org.hibernate.query.criteria.JpaTupleElement;
import org.hibernate.query.sqm.tree.spi.SqmCopyContext;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteTable;
import org.hibernate.query.sqm.tree.spi.cte.SqmCteTableColumn;
import org.hibernate.query.sqm.tree.spi.expression.SqmExpression;
import org.hibernate.query.sqm.tree.spi.expression.SqmNumericExpressionWrapper;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(annotatedClasses = CriteriaJavaTypeTest.Item.class)
@SessionFactory
class CriteriaJavaTypeTest {
	@BeforeEach
	void prepareData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from JavaTypeItem" ).executeUpdate();
			final var item = new Item();
			item.id = 1;
			item.name = "item";
			item.status = Status.ACTIVE;
			item.ordinalStatus = Status.ACTIVE;
			session.persist( item );
		} );
	}

	@Test
	void unresolvedExpressions(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		for ( var expression : new JpaTupleElement<?>[] {
				builder.coalesce(), builder.selectCase(), builder.selectCase( builder.literal( 1 ) ),
				builder.value( null ), builder.literal( null )
		} ) {
			assertThrows( IllegalStateException.class, expression::getJavaType );
			assertNull( expression.getJavaTypeIfKnown() );
		}
	}

	@Test
	void typedNullLiterals(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		for ( var type : new Class<?>[] { String.class, Item.class, Status.class } ) {
			final var literal = (SqmExpression<?>) builder.nullLiteral( type );
			assertSame( type, literal.getJavaType() );
			assertSame( type, literal.copy( SqmCopyContext.simpleContext() ).getJavaType() );
		}
	}

	@Test
	void enumNullLiteralKeepsContextualMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			for ( var attribute : new String[] { "status", "ordinalStatus" } ) {
				final var criteria = builder.createQuery( Item.class );
				final var root = criteria.from( Item.class );
				final var literal = builder.nullLiteral( Status.class );
				criteria.select( root ).where( builder.equal( root.get( attribute ), literal ) );
				assertEquals( 0, session.createQuery( criteria ).getResultList().size() );
				assertSame( Status.class, literal.getJavaType() );
			}
		} );
	}

	@Test
	void coalesceInfersType(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		final var coalesce = builder.<String>coalesce();
		assertThrows( IllegalStateException.class, coalesce::getJavaType );
		coalesce.value( "first" ).value( "second" );
		assertSame( String.class, coalesce.getJavaType() );
		assertSame( String.class, ((SqmExpression<?>) coalesce).copy( SqmCopyContext.simpleContext() ).getJavaType() );
		assertSame( Long.class, builder.<Number>coalesce().value( builder.literal( 1 ) ).value( builder.literal( 2L ) ).getJavaType() );
		assertSame( Long.class, builder.<Number>coalesce().value( builder.literal( 2L ) ).value( builder.literal( 1 ) ).getJavaType() );
	}

	@Test
	void coalesceWithNullArgument(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( String.class );
			final var root = criteria.from( Item.class );
			final var coalesce = builder.<String>coalesce().value( (String) null ).value( root.<String>get( "name" ) );
			assertSame( String.class, coalesce.getJavaType() );
			criteria.select( coalesce );
			assertEquals( "item", session.createQuery( criteria ).getSingleResult() );
		} );
	}

	@Test
	void caseInfersType(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		assertSame( String.class, builder.<String>selectCase()
				.when( builder.conjunction(), "yes" ).otherwise( "no" ).getJavaType() );
		assertSame( String.class, builder.<Integer, String>selectCase( builder.literal( 1 ) )
				.when( 1, "yes" ).otherwise( "no" ).getJavaType() );
	}

	@Test
	void typedExpressions(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		final var criteria = builder.createQuery( String.class );
		final var root = criteria.from( Item.class );
		assertSame( Item.class, root.getJavaType() );
		assertSame( String.class, root.get( "name" ).getJavaType() );
		assertSame( String.class, criteria.subquery( String.class ).getJavaType() );
		assertSame( Item.class, criteria.subquery( Item.class ).getJavaType() );
		assertSame( String.class, builder.function( "lower", String.class, root.get( "name" ) ).getJavaType() );
		assertSame( Tuple.class, builder.tuple( root.get( "name" ), root.get( "id" ) ).getJavaType() );
		assertSame( Object[].class, builder.array( root.get( "name" ), root.get( "id" ) ).getJavaType() );
	}

	@Test
	void castUnresolvedExpression(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		final var expression = new SqmNumericExpressionWrapper<>( (SqmExpression<Integer>) builder.<Integer>value( null ) );
		assertThrows( IllegalStateException.class, expression::getJavaType );
		assertSame( Long.class, expression.asLong().getJavaType() );
		assertSame( Integer.class, expression.asInteger().getJavaType() );
		assertSame( Double.class, expression.asDouble().getJavaType() );
	}

	@Test
	void tupleWithUntypedNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createTupleQuery();
			final var root = criteria.from( Item.class );
			final var nullLiteral = builder.literal( null );
			criteria.multiselect( root.get( "name" ), nullLiteral );
			final var tuple = session.createQuery( criteria ).getSingleResult();
			assertEquals( "item", tuple.get( 0 ) );
			assertNull( tuple.get( nullLiteral ) );
			assertThrows( IllegalStateException.class, nullLiteral::getJavaType );
		} );
	}

	@Test
	void singleUntypedNullRemainsScalar(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertNull( session.createQuery( "select null from JavaTypeItem", String.class ).getSingleResult() );
			assertNull( session.createQuery( "select null from JavaTypeItem", SingleValue.class ).getSingleResult() );
		} );
	}

	@Test
	void explicitConstructorWithUntypedNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createQuery(
					"select new " + SingleValue.class.getName() + "(null) from JavaTypeItem", SingleValue.class )
					.getSingleResult();
			assertNull( result.value() );
		} );
	}

	@Test
	void constructorWithUntypedNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var result = session.createQuery( "select name, null from JavaTypeItem", Result.class ).getSingleResult();
			assertEquals( "item", result.name() );
			assertNull( result.status() );
		} );
	}

	@Test
	void nullCannotMatchPrimitiveConstructorArgument(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThrows( org.hibernate.InstantiationException.class,
				() -> session.createQuery( "select name, null from JavaTypeItem", PrimitiveResult.class ).getSingleResult() ) );
	}

	@Test
	void cteColumnTypes(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		final var definition = builder.createTupleQuery();
		final var root = definition.from( Item.class );
		definition.multiselect( root.get( "name" ).alias( "name" ) );
		final var cte = builder.createTupleQuery().with( definition );
		assertSame( String.class, cte.getType().getAttribute( "name" ).getJavaType() );
		final var unresolved = new SqmCteTableColumn( (SqmCteTable<?>) cte.getType(), "unknown", null );
		assertThrows( IllegalStateException.class, unresolved::getJavaType );
	}

	@Test
	void windowFunctionWithUnresolvedArgument(SessionFactoryScope scope) {
		final var builder = scope.getSessionFactory().getCriteriaBuilder();
		final var expression = builder.firstValue( builder.value( null ), builder.createWindow() );
		assertThrows( IllegalStateException.class, expression::getJavaType );
	}

	@Test
	void constructorWithEnumNull(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Result.class );
			final var root = criteria.from( Item.class );
			criteria.multiselect( root.get( "name" ), builder.nullLiteral( Status.class ) );
			final var result = session.createQuery( criteria ).getSingleResult();
			assertEquals( "item", result.name() );
			assertNull( result.status() );
		} );
	}

	@Test
	void nullValueParameterCanStillBeUsed(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var builder = session.getCriteriaBuilder();
			final var criteria = builder.createQuery( Item.class );
			final var root = criteria.from( Item.class );
			final Expression<Object> value = builder.value( null );
			criteria.select( root ).where( value.isNull() );
			assertEquals( 1, session.createQuery( criteria ).getResultList().size() );
		} );
	}

	public record Result(String name, Status status) {}

	public record SingleValue(String value) {}

	public record PrimitiveResult(String name, int count) {}

	enum Status { ACTIVE }

	@Entity(name = "JavaTypeItem")
	static class Item {
		@Id
		Integer id;
		String name;
		@Enumerated(EnumType.STRING)
		Status status;
		@Enumerated(EnumType.ORDINAL)
		Status ordinalStatus;
	}
}
