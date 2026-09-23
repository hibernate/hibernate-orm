/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.relational.naming.internal;

import java.io.Serializable;

import org.hibernate.Internal;
import org.hibernate.relational.naming.spi.PhysicalName;

/// Data-only serial form of a finalized physical name, without its comparison policy.
///
/// @author Steve Ebersole
@Internal
public record PhysicalNameSnapshot(String text, boolean quoted) implements Serializable {
	public static PhysicalNameSnapshot from(PhysicalName name) {
		return name == null ? null : new PhysicalNameSnapshot( name.getText(), name.isQuoted() );
	}

	/// Reconstruct under the restored system's policy without naming or quote normalization.
	public PhysicalName restore(PhysicalName.Factory factory) {
		return factory.create( text, quoted );
	}
}
