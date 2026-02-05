/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.find;

import org.hibernate.engine.spi.StatelessSessionImplementor;

/// Context for performing load operations from [stateless sessions][StatelessSessionImplementor].
///
/// @author Steve Ebersole
public interface StatelessLoadAccessContext extends LoadAccessContext {
	StatelessSessionImplementor getStatelessSession();

	@Override
	default StatelessSessionImplementor getEntityHandler() {
		return getStatelessSession();
	}
}
