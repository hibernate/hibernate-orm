/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.sql.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

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
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final Long size = columnSize.getLength();
		final Integer precision = columnSize.getPrecision();
		final Integer scale = columnSize.getScale();
		return precision != null
				? formatTypeName( size, (int) (precision / LOG_BASE2OF10), scale )
				: formatTypeName( size, null, scale );
	}
}
