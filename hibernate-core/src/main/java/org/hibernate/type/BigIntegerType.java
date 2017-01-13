/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.math.BigInteger;

import org.hibernate.type.spi.JdbcLiteralFormatter;
import org.hibernate.type.spi.basic.BasicTypeImpl;
import org.hibernate.type.descriptor.java.internal.BigIntegerJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.NumericSqlDescriptor;

/**
 * A type that maps between a {@link java.sql.Types#NUMERIC NUMERIC} and {@link BigInteger}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BigIntegerType
		extends BasicTypeImpl<BigInteger> {

	public static final BigIntegerType INSTANCE = new BigIntegerType();

	public BigIntegerType() {
		super( BigIntegerJavaDescriptor.INSTANCE, NumericSqlDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_integer";
	}

	@Override
	public JdbcLiteralFormatter<BigInteger> getJdbcLiteralFormatter() {
		return NumericSqlDescriptor.INSTANCE.getJdbcLiteralFormatter( BigIntegerJavaDescriptor.INSTANCE );
	}
}
