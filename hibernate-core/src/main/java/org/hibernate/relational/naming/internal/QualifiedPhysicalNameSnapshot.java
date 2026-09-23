/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.naming.internal;

import java.io.Serializable;

import org.hibernate.Internal;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.QualifiedPhysicalName;

/// Data-only serial form of a finalized qualified physical name.
///
/// @author Steve Ebersole
@Internal
public record QualifiedPhysicalNameSnapshot(
		PhysicalNameSnapshot catalog, PhysicalNameSnapshot schema, PhysicalNameSnapshot object)
		implements Serializable {
	public static QualifiedPhysicalNameSnapshot from(QualifiedPhysicalName name) {
		return new QualifiedPhysicalNameSnapshot( PhysicalNameSnapshot.from( name.catalogName() ),
				PhysicalNameSnapshot.from( name.schemaName() ), PhysicalNameSnapshot.from( name.objectName() ) );
	}

	public QualifiedPhysicalName restore(PhysicalName.Factory factory) {
		return new QualifiedPhysicalName( catalog == null ? null : catalog.restore( factory ),
				schema == null ? null : schema.restore( factory ), object.restore( factory ) );
	}
}
