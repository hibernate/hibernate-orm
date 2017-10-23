/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public abstract class AbstractHolderTest extends BaseUnitTestCase {
	protected abstract IntegralDataTypeHolder makeHolder();

	@Test
	@SuppressWarnings( {"EmptyCatchBlock"})
	public void testInitializationChecking() {
		IntegralDataTypeHolder holder = makeHolder();
		try {
			holder.increment();
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.add( 1 );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.decrement();
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.subtract( 1 );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.multiplyBy( holder );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.multiplyBy( 1 );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.eq( holder );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.eq( 1 );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.lt( holder );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.lt( 1 );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.gt( holder );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.gt( 1 );
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}

		try {
			holder.makeValue();
			fail();
		}
		catch ( IdentifierGenerationException expected ) {
		}
	}

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
