/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;
import java.math.BigInteger;

import org.hibernate.type.descriptor.java.BigIntegerJavaType;

/**
 * @author Steve Ebersole
 */
public class BigIntegerDescriptorTest extends AbstractDescriptorTest<BigInteger> {
	final BigInteger original = BigInteger.valueOf( 100 );
	final BigInteger copy = BigInteger.valueOf( 100 );
	final BigInteger different = BigInteger.valueOf( 999 );

	public BigIntegerDescriptorTest() {
		super( BigIntegerJavaType.INSTANCE );
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
