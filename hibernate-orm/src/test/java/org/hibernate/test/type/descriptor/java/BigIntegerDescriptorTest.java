/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;
import java.math.BigInteger;

import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BigIntegerDescriptorTest extends AbstractDescriptorTest<BigInteger> {
	final BigInteger original = BigInteger.valueOf( 100 );
	final BigInteger copy = BigInteger.valueOf( 100 );
	final BigInteger different = BigInteger.valueOf( 999 );

	public BigIntegerDescriptorTest() {
		super( BigIntegerTypeDescriptor.INSTANCE );
	}

	@Override
	protected Data<BigInteger> getTestData() {
		return new Data<BigInteger>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
}
