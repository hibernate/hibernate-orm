/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.internal.InternalContract;
import org.hibernate.dialect.spi.ImplementableContract;
import org.hibernate.dialect.spi.UseOnlyContract;

/// Provider hierarchy fixture with valid implementable, invalid use-only, and
/// invalid upstream-internal interface targets.
///
/// @author Steve Ebersole
public class HierarchyDialect extends Dialect
		implements ImplementableContract, UseOnlyContract, InternalContract {
}
