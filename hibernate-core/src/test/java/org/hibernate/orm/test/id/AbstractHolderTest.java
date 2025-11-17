/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id;

import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IntegralDataTypeHolder;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
@BaseUnitTest
public abstract class AbstractHolderTest {
	protected abstract IntegralDataTypeHolder makeHolder();

	@Test
	@SuppressWarnings({ "EmptyCatchBlock" })
	public void testInitializationChecking() {
		IntegralDataTypeHolder holder = makeHolder();
		try {
			holder.increment();
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.add( 1 );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.decrement();
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.subtract( 1 );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.multiplyBy( holder );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.multiplyBy( 1 );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.eq( holder );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.eq( 1 );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.lt( holder );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.lt( 1 );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.gt( holder );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.gt( 1 );
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}

		try {
			holder.makeValue();
			fail();
		}
		catch (IdentifierGenerationException expected) {
		}
	}

	@Test
	public void testIncrement() {
		IntegralDataTypeHolder holder = makeHolder();
		holder.initialize( 0 );
		int i = 0;
		for ( ; i < 5008; i++ ) {
			holder.increment();
		}
		assertEquals( holder.copy().initialize( i ), holder );
	}

	@Test
	public void testBasicHiloAlgorithm() {
		// mimic an initialValue of 1 and increment of 20
		final long initialValue = 1;
		final long incrementSize = 2;

		// initialization
		IntegralDataTypeHolder lastSourceValue = makeHolder().initialize( 1 );
		IntegralDataTypeHolder upperLimit = lastSourceValue.copy().multiplyBy( incrementSize ).increment();
		IntegralDataTypeHolder value = upperLimit.copy().subtract( incrementSize );

		assertEquals( 1, lastSourceValue.makeValue().longValue() );
		assertEquals( 3, upperLimit.makeValue().longValue() );
		assertEquals( 1, value.makeValue().longValue() );

		value.increment();
		value.increment();

		assertFalse( upperLimit.gt( value ) );

		// at which point we would "clock over"
		lastSourceValue.increment();
		upperLimit = lastSourceValue.copy().multiplyBy( incrementSize ).increment();

		assertEquals( 2, lastSourceValue.makeValue().longValue() );
		assertEquals( 5, upperLimit.makeValue().longValue() );
		assertEquals( 3, value.makeValue().longValue() );
	}
}
