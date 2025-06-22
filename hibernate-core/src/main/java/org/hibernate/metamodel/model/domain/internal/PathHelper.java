/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;

public class PathHelper {
	public static NavigablePath append(SqmPath<?> lhs, SqmPathSource<?> rhs, SqmPathSource<?> intermediatePathSource) {
		return intermediatePathSource == null
				? lhs.getNavigablePath().append( rhs.getPathName() )
				: lhs.getNavigablePath().append( intermediatePathSource.getPathName() ).append( rhs.getPathName() );
	}
}
