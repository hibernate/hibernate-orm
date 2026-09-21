/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.relational.internal;

import java.io.Serializable;

import org.hibernate.Internal;
import org.hibernate.boot.model.relational.PhysicalNamespaceName;
import org.hibernate.relational.naming.internal.PhysicalNameSnapshot;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Serial namespace state which never captures the system's comparison policy.
///
/// @author Steve Ebersole
@Internal
public record PhysicalNamespaceSnapshot(PhysicalNameSnapshot catalog, PhysicalNameSnapshot schema)
		implements Serializable {
	public static PhysicalNamespaceSnapshot from(PhysicalNamespaceName name) {
		return new PhysicalNamespaceSnapshot(
				PhysicalNameSnapshot.from( name.catalog() ), PhysicalNameSnapshot.from( name.schema() ) );
	}

	public PhysicalNamespaceName restore(PhysicalName.Factory factory) {
		return new PhysicalNamespaceName(
				catalog == null ? null : catalog.restore( factory ),
				schema == null ? null : schema.restore( factory ) );
	}
}
