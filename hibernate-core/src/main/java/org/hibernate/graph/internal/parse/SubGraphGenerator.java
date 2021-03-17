/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
