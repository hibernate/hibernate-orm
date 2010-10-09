/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id;

import junit.framework.TestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public abstract class AbstractHolderTest extends TestCase {
	protected abstract IntegralDataTypeHolder makeHolder();

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
