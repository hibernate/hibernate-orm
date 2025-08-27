/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.hana;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.BasicTypeReference;

public class HANASpatialAggregate extends StandardSQLFunction {

	public HANASpatialAggregate(String name) {
		super( name );
	}

	public HANASpatialAggregate(String name, BasicTypeReference<?> registeredType) {
		super( name, registeredType );
	}
}
