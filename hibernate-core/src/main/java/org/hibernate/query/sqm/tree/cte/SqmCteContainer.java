/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.cte;

import java.util.Collection;

import org.hibernate.query.criteria.JpaCteContainer;
import org.hibernate.query.sqm.tree.SqmNode;

/**
 * @author Christian Beikov
 */
public interface SqmCteContainer extends SqmNode, JpaCteContainer {

	Collection<SqmCteStatement<?>> getCteStatements();

	SqmCteStatement<?> getCteStatement(String cteLabel);

}
