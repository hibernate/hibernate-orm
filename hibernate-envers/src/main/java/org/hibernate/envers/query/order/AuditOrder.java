/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.order;

import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.tools.Pair;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface AuditOrder {
	/**
	 * @param enversService The EnversService
	 *
	 * @return A pair: (property name, ascending?).
	 */
	Pair<String, Boolean> getData(EnversService enversService);
}
