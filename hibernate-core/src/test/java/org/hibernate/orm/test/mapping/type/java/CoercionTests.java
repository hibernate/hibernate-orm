/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;
import org.hibernate.type.descriptor.java.CoercionException;
import org.hibernate.type.descriptor.java.CoercionHelper;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import org.hamcrest.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for implicit widening coercions
 *
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = CoercionTests.TheEntity.class )
@SessionFactory
public class CoercionTests {
	final short shortValue = 1;
	final byte byteValue = 1;
	final int intValue = 1;
	final long longValue = 1L;
	final double doubleValue = 0.5;
	final float floatValue = 0.5F;

	final long largeLongValue = Integer.MAX_VALUE + 1L;
	final float largeFloatValue = (float) Double.MAX_VALUE + 1.5F;

	@Test
	public void testCoercibleDetection(SessionFactoryScope scope) {
		final TypeConfiguration typeConfiguration = scope.getSessionFactory().getTypeConfiguration();
		final JavaTypeRegistry jtdRegistry = typeConfiguration.getJavaTypeRegistry();

		final JavaType<Integer> integerType = jtdRegistry.resolveDescriptor( Integer.class );
		final JavaType<Long> longType = jtdRegistry.resolveDescriptor( Long.class );
		final JavaType<Double> doubleType = jtdRegistry.resolveDescriptor( Double.class );
		final JavaType<Float> floatType = jtdRegistry.resolveDescriptor( Float.class );

		scope.inTransaction(
				(session) -> {
					checkIntegerConversions( integerType, session );
					checkLongConversions( longType, session );

					checkDoubleConversions( doubleType, session );
				}
		);
	}

	private void checkDoubleConversions(JavaType<Double> doubleType, SessionImplementor session) {
		assertThat( doubleType.coerce( (double) 1, session ), Matchers.is( 1.0 ) );
		assertThat( doubleType.coerce( 1F, session ), Matchers.is( 1.0 ) );
		assertThat( doubleType.coerce( doubleValue, session ), Matchers.is( doubleValue ) );
		assertThat( doubleType.coerce( floatValue, session ), Matchers.is( doubleValue ) );

		assertThat( doubleType.coerce( largeFloatValue, session ), Matchers.is( (double) largeFloatValue ) );

		assertThat( doubleType.coerce( shortValue, session ), Matchers.is( 1.0 ) );
		assertThat( doubleType.coerce( byteValue, session ), Matchers.is( 1.0 ) );
		assertThat( doubleType.coerce( longValue, session ), Matchers.is( 1.0 ) );

		assertThat( doubleType.coerce( BigInteger.ONE, session ), Matchers.is( 1.0 ) );
		assertThat( doubleType.coerce( BigDecimal.ONE, session ), Matchers.is( 1.0 ) );

		// negative checks
	}

	private void checkIntegerConversions(JavaType<Integer> integerType, SessionImplementor session) {
		assertThat( integerType.coerce( intValue, session ), Matchers.is( intValue) );

		assertThat( integerType.coerce( shortValue, session ), Matchers.is( intValue) );
		assertThat( integerType.coerce( byteValue, session ), Matchers.is( intValue) );

		assertThat( integerType.coerce( longValue, session ), Matchers.is( intValue) );

		assertThat( integerType.coerce( (double) 1, session ), Matchers.is( intValue) );
		assertThat( integerType.coerce( 1F, session ), Matchers.is( intValue) );

		assertThat( integerType.coerce( BigInteger.ONE, session ), Matchers.is( intValue) );
		assertThat( integerType.coerce( BigDecimal.ONE, session ), Matchers.is( intValue) );

		// negative checks
		checkDisallowedConversion( () -> integerType.coerce( largeLongValue, session ) );
		checkDisallowedConversion( () -> integerType.coerce( largeFloatValue, session ) );
		checkDisallowedConversion( () -> integerType.coerce( doubleValue, session ) );
		checkDisallowedConversion( () -> integerType.coerce( floatValue, session ) );
	}

	private void checkLongConversions(JavaType<Long> longType, SessionImplementor session) {
		assertThat( longType.coerce( longValue, session ), Matchers.is( longValue ) );
		assertThat( longType.coerce( largeLongValue, session ), Matchers.is( largeLongValue ) );

		assertThat( longType.coerce( intValue, session ), Matchers.is( longValue ) );
		assertThat( longType.coerce( shortValue, session ), Matchers.is( longValue ) );
		assertThat( longType.coerce( byteValue, session ), Matchers.is( longValue ) );

		assertThat( longType.coerce( (double) 1, session ), Matchers.is( longValue ) );
		assertThat( longType.coerce( 1F, session ), Matchers.is( longValue ) );

		assertThat( longType.coerce( BigInteger.ONE, session ), Matchers.is( longValue ) );
		assertThat( longType.coerce( BigDecimal.ONE, session ), Matchers.is( longValue ) );

		// negative checks
		checkDisallowedConversion( () -> longType.coerce( largeFloatValue, session ) );
		checkDisallowedConversion( () -> longType.coerce( doubleValue, session ) );
		checkDisallowedConversion( () -> longType.coerce( floatValue, session ) );
	}

	private void checkDisallowedConversion(CoercionHelper.Coercer callback) {
		try {
			callback.doCoercion();
			fail( "Expecting coercion to fail" );
		}
		catch (CoercionException expected) {
		}
	}

	@Test
	public void testLoading(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.byId( TheEntity.class ).load( 1L );

					session.byId( TheEntity.class ).load( (byte) 1 );
					session.byId( TheEntity.class ).load( (short) 1 );
					session.byId( TheEntity.class ).load( 1 );

					session.byId( TheEntity.class ).load( 1.0 );
					session.byId( TheEntity.class ).load( 1.0F );

					session.byId( TheEntity.class ).load( BigInteger.ONE );
					session.byId( TheEntity.class ).load( BigDecimal.ONE );
				}
		);
		scope.inTransaction(
				(session) -> {
					session.byId( TheEntity.class ).getReference( 1L );
					session.byId( TheEntity.class ).getReference( 1 );
				}
		);
	}

	@Test
	public void testMultiIdLoading(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.byMultipleIds( TheEntity.class ).multiLoad( 1L );

					session.byMultipleIds( TheEntity.class ).multiLoad( (byte) 1 );
					session.byMultipleIds( TheEntity.class ).multiLoad( (short) 1 );
					session.byMultipleIds( TheEntity.class ).multiLoad( 1 );

					session.byMultipleIds( TheEntity.class ).multiLoad( 1.0 );
					session.byMultipleIds( TheEntity.class ).multiLoad( 1.0F );

					session.byMultipleIds( TheEntity.class ).multiLoad( BigInteger.ONE );
					session.byMultipleIds( TheEntity.class ).multiLoad( BigDecimal.ONE );
				}
		);
		scope.inTransaction(
				(session) -> {
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1L ) );
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1 ) );
				}
		);
	}

	@Test
	public void testNaturalIdLoading(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.bySimpleNaturalId( TheEntity.class ).load( 1L );

					session.bySimpleNaturalId( TheEntity.class ).load( (byte) 1 );
					session.bySimpleNaturalId( TheEntity.class ).load( (short) 1 );
					session.bySimpleNaturalId( TheEntity.class ).load( 1 );

					session.bySimpleNaturalId( TheEntity.class ).load( 1.0 );
					session.bySimpleNaturalId( TheEntity.class ).load( 1.0F );

					session.bySimpleNaturalId( TheEntity.class ).load( BigInteger.ONE );
					session.bySimpleNaturalId( TheEntity.class ).load( BigDecimal.ONE );
				}
		);
		scope.inTransaction(
				(session) -> {
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1L ) );
					session.byMultipleIds( TheEntity.class ).multiLoad( Arrays.asList( 1 ) );
				}
		);
	}

	@Test
	public void testMultiNaturalIdLoading(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( 1L );

					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( (byte) 1 );
					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( (short) 1 );
					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( 1 );

					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( 1.0 );
					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( 1.0F );

					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( BigInteger.ONE );
					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( BigDecimal.ONE );
				}
		);
		scope.inTransaction(
				(session) -> {
					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( Arrays.asList( 1L ) );
					session.byMultipleNaturalId( TheEntity.class ).enableOrderedReturn( false ).multiLoad( Arrays.asList( 1 ) );
				}
		);
	}

	@Test
	public void testQueryParameterIntegralWiden(SessionFactoryScope scope) {
		final String qry = "select e from TheEntity e where e.longId = :id";

		scope.inTransaction(
				(session) -> {
					final Query query = session.createQuery( qry );

					query.setParameter( "id", 1L ).list();

					query.setParameter( "id", 1 ).list();
				}
		);
	}

	@Test
	public void testQueryParameterIntegralNarrow(SessionFactoryScope scope) {
		final String qry = "select e from TheEntity e where e.intValue = ?1";

		scope.inTransaction(
				(session) -> {
					final Query query = session.createQuery( qry );

					query.setParameter( 1, 1 ).list();

					query.setParameter( 1, 1L ).list();
				}
		);
	}

	@Test
	public void testQueryParameterFloatingWiden(SessionFactoryScope scope) {
		final String qry = "select e from TheEntity e where e.floatValue = :p";

		scope.inTransaction(
				(session) -> {
					final Query query = session.createQuery( qry );

					query.setParameter( "p", 0.5f ).list();

					query.setParameter( "p", 0.5 ).list();
				}
		);
	}

	@Test
	public void testQueryParameterFloatingNarrow(SessionFactoryScope scope) {
		final String qry = "select e from TheEntity e where e.doubleValue = :p";

		scope.inTransaction(
				(session) -> {
					final Query query = session.createQuery( qry );

					query.setParameter( "p", 0.5 ).list();

					query.setParameter( "p", 0.5f ).list();
				}
		);
	}

	@Entity( name = "TheEntity" )
	@Table( name = "the_entity" )
	public static class TheEntity {
		@Id
		private Long longId;
		@NaturalId
		private Long longNaturalId;
		private Integer intValue;
		private Float floatValue;
		private Double doubleValue;

	}
}
