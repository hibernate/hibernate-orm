/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
