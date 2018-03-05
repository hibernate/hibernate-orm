/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi;

import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public interface ParameterInformation {
	/**
	 * The positions (relative to all parameters) that this parameter
	 * was discovered in the source query (HQL, etc).  E.g., given a query
	 * like `.. where a.name = :name or a.nickName = :name` this would
	 * return `[0,1]`
	 */
	int[] getSourceLocations();

	Type getExpectedType();

	void setExpectedType(Type expectedType);

	void addSourceLocation(int position);
}
