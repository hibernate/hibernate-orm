/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;
import java.math.BigDecimal;

import org.hibernate.type.descriptor.java.BigDecimalTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BigDecimalDescriptorTest extends AbstractDescriptorTest<BigDecimal> {
	final BigDecimal original = new BigDecimal( 100 );
	final BigDecimal copy = new BigDecimal( 100 );
	final BigDecimal different = new BigDecimal( 999 );

	public BigDecimalDescriptorTest() {
		super( BigDecimalTypeDescriptor.INSTANCE );
	}

	@Override
	protected Data<BigDecimal> getTestData() {
		return new Data<BigDecimal>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
}
