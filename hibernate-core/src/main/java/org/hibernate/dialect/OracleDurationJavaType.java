/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import oracle.sql.INTERVALDS;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.DurationJavaType;

import java.time.Duration;

public class OracleDurationJavaType extends DurationJavaType {

	public static final OracleDurationJavaType INSTANCE = new OracleDurationJavaType();

	public OracleDurationJavaType() {
		super();
	}

	@Override
	public <X> Duration wrap(X value, WrapperOptions options) {
		if(value == null) {
			return null;
		}

		if ( value instanceof INTERVALDS ) {
			return INTERVALDS.toDuration( ((INTERVALDS) value).toBytes() );
		}

		return super.wrap( value, options );
	}
}
