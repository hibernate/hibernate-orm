/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.mapping.internal;

import jakarta.persistence.PessimisticLockScope;

/// JAXB marshaling for [PessimisticLockScope]
///
/// @author Steve Ebersole
public class PessimisticLockScopeMarshalling {
	public static PessimisticLockScope fromXml(String name) {
		return name == null ? null : PessimisticLockScope.valueOf( name );
	}

	public static String toXml(PessimisticLockScope lockScope) {
		return lockScope == null ? null : lockScope.name();
	}
}
