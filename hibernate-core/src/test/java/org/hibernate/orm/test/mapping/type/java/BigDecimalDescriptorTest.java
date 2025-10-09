/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.java;
import java.math.BigDecimal;

import org.hibernate.type.descriptor.java.BigDecimalJavaType;

/**
 * @author Steve Ebersole
 */
public class BigDecimalDescriptorTest extends AbstractDescriptorTest<BigDecimal> {
	final BigDecimal original = new BigDecimal( 100 );
	final BigDecimal copy = new BigDecimal( 100 );
	final BigDecimal different = new BigDecimal( 999 );

	public BigDecimalDescriptorTest() {
		super( BigDecimalJavaType.INSTANCE );
	}

	@Override
	protected Data<BigDecimal> getTestData() {
		return new Data<>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
}
