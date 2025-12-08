/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

public class PathHelper {
	public static NavigablePath append(SqmPath<?> lhs, SqmPathSource<?> rhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		final var navigablePath = lhs.getNavigablePath();
		return intermediatePathSource == null
				? navigablePath.append( rhs.getPathName() )
				: navigablePath.append( intermediatePathSource.getPathName() ).append( rhs.getPathName() );
	}
}
