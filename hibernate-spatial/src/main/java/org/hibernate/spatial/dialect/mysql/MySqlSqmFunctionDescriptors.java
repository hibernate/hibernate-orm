/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.mysql;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.spatial.BaseSqmFunctionDescriptors;
import org.hibernate.spatial.CommonSpatialFunction;

public class MySqlSqmFunctionDescriptors extends BaseSqmFunctionDescriptors {

	private static final Set<CommonSpatialFunction> UNSUPPORTED = EnumSet.of(
			CommonSpatialFunction.ST_BOUNDARY, CommonSpatialFunction.ST_RELATE );

	public MySqlSqmFunctionDescriptors(FunctionContributions functionContributions) {
		super( functionContributions );
	}

	@Override
	public CommonSpatialFunction[] filter(CommonSpatialFunction[] functions) {
		return Arrays.stream( functions )
				.filter( f -> !UNSUPPORTED.contains( f ) )
				.toArray( CommonSpatialFunction[]::new );
	}
}
