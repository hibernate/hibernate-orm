/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi.criteria.from;

import java.util.LinkedHashSet;

/**
 * @author Steve Ebersole
 */
public interface JpaFromClause {
	/**
	 * NOTE : Exposed as LinkedHashSet because it is both unique and ordered
	 */
	LinkedHashSet<JpaRoot<?>> getRoots();
}
