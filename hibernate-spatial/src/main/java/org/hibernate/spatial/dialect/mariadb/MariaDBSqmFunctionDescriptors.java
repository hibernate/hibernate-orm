/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.mariadb;

import java.util.Arrays;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.spatial.BaseSqmFunctionDescriptors;
import org.hibernate.spatial.CommonSpatialFunction;

public class MariaDBSqmFunctionDescriptors extends BaseSqmFunctionDescriptors {
	public MariaDBSqmFunctionDescriptors(FunctionContributions functionContributions) {
		super( functionContributions );
	}

	@Override
	public CommonSpatialFunction[] filter(CommonSpatialFunction[] functions) {
		return Arrays.stream( functions )
				//todo -- investigate in more detail why st_relate fails
				.filter(f -> f != CommonSpatialFunction.ST_RELATE )
				.toArray( CommonSpatialFunction[]::new);
	}
}
