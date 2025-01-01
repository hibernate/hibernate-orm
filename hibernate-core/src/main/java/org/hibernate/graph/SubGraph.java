/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph;

import jakarta.persistence.Subgraph;

/**
 * Extends the JPA-defined {@link Subgraph} with additional operations.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @see RootGraph
 */
public interface SubGraph<J> extends Graph<J>, Subgraph<J> {
	@Override
	default Class<J> getClassType() {
		return getGraphedType().getJavaType();
	}
}
