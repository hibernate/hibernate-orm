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
package org.hibernate.type;

import java.math.BigInteger;
import java.sql.Types;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.java.BigIntegerTypeDescriptor;
import org.hibernate.type.descriptor.sql.NumericTypeDescriptor;

/**
 * A type that maps between a {@link Types#NUMERIC NUMERIC} and {@link BigInteger}.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class BigIntegerType
		extends AbstractSingleColumnStandardBasicType<BigInteger>
		implements DiscriminatorType<BigInteger> {

	public static final BigIntegerType INSTANCE = new BigIntegerType();

	public BigIntegerType() {
		super( NumericTypeDescriptor.INSTANCE, BigIntegerTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "big_integer";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}

	@Override
	public String objectToSQLString(BigInteger value, Dialect dialect) {
		return BigIntegerTypeDescriptor.INSTANCE.toString( value );
	}

	@Override
	public BigInteger stringToObject(String string) {
		return BigIntegerTypeDescriptor.INSTANCE.fromString( string );
	}
}
