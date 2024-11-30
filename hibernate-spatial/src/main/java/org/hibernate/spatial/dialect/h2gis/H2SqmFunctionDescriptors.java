/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.h2gis;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.spatial.BaseSqmFunctionDescriptors;

public class H2SqmFunctionDescriptors extends BaseSqmFunctionDescriptors {
	public H2SqmFunctionDescriptors(FunctionContributions contributions) {
		super( contributions );
	}
}
