/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.UUID;

/**
 * @author Jan Schatteman
 */
public class OracleUUIDJavaType extends UUIDJavaType {

	/* This class is related to the changes that were made for HHH-17246 */

	public static final OracleUUIDJavaType INSTANCE = new OracleUUIDJavaType();

	@Override
	public String toString(UUID value) {
		return NoDashesStringTransformer.INSTANCE.transform( value );
	}

	@Override
	public UUID fromString(CharSequence string)  {
		return NoDashesStringTransformer.INSTANCE.parse( string.toString() );
	}
}
