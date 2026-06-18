/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

public class PathHelper {
	public static NavigablePath append(SqmPath<?> lhs, SqmPathSource<?> rhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		final var navigablePath = lhs.getNavigablePath();
		return intermediatePathSource == null
				? navigablePath.append( rhs.getPathName() )
				: navigablePath.append( intermediatePathSource.getPathName() ).append( rhs.getPathName() );
	}
}
