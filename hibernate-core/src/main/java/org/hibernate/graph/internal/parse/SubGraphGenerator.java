/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.AttributeNodeImplementor;
import org.hibernate.graph.spi.SubGraphImplementor;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface SubGraphGenerator {
	SubGraphImplementor<?> createSubGraph(
			AttributeNodeImplementor<?> attributeNode,
			String subTypeName,
			SessionFactoryImplementor sessionFactory);
}
