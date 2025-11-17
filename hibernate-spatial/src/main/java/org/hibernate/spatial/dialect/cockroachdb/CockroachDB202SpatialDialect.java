/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.cockroachdb;

import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.spatial.SpatialDialect;

/**
 * An @{code SpatialDialect} for CockroachDB 20.2 and later. CockroachDB's spatial features where introduced in
 * that version.
 * @deprecated Spatial Dialects are no longer needed. Use the standard CockroachDB dialects
 */
@Deprecated
public class CockroachDB202SpatialDialect extends CockroachDialect implements SpatialDialect {
	public CockroachDB202SpatialDialect() {
		super( DatabaseVersion.make( 19, 2 ) );
	}
}
