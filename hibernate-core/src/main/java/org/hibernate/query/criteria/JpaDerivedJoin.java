/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;

/**
 * @author Christian Beikov
 */
@Incubating
public interface JpaDerivedJoin<T> extends JpaDerivedFrom<T>, SqmQualifiedJoin<T,T>, JpaJoinedFrom<T,T> {

	/**
	 * Specifies whether the subquery part can access previous from node aliases.
	 * Normally, subqueries in the from clause are unable to access other from nodes,
	 * but when specifying them as lateral, they are allowed to do so.
	 * Refer to the SQL standard definition of LATERAL for more details.
	 */
	boolean isLateral();

}
