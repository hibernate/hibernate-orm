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

public class Scale6IntervalSecondDdlType extends DdlTypeImpl {

	public Scale6IntervalSecondDdlType(Dialect dialect) {
		this( "interval second($s)", dialect );
	}

	public Scale6IntervalSecondDdlType(String typeNamePattern, Dialect dialect) {
		super( SqlTypes.INTERVAL_SECOND, typeNamePattern, dialect );
	}

	@Override
	public String getTypeName(Size columnSize, Type type, DdlTypeRegistry ddlTypeRegistry) {
		final Integer scale = columnSize.getScale();
		// The maximum scale for `interval second` is 6 unfortunately
		if ( scale == null || scale > 6 ) {
			throw new IllegalStateException( "Illegal attempt to use interval second type with scale > 6" );
		}
		return formatTypeName( columnSize.getLength(), columnSize.getPrecision(), scale );
	}
}
