/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.SqlTypes;

import static java.lang.Math.log;

public class BinaryFloatDdlType extends DdlTypeImpl {

	//needed for converting precision from decimal to binary digits
	private static final double LOG_BASE2OF10 = log(10)/log(2);

	public BinaryFloatDdlType(Dialect dialect) {
		this( "float($p)", dialect );
	}

	public BinaryFloatDdlType(String typeNamePattern, Dialect dialect) {
		super( SqlTypes.FLOAT, typeNamePattern, dialect );
	}

	@Override
	public String getTypeName(Long size, Integer precision, Integer scale) {
		if ( precision != null ) {
			return super.getTypeName( size, (int) ( precision / LOG_BASE2OF10 ), scale );
		}
		return super.getTypeName( size, precision, scale );
	}
}
