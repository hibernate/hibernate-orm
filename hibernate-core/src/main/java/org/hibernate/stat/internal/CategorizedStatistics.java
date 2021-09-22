/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.stat.internal;

import java.io.Serializable;

/**
 * Statistics for a particular "category" (a named entity, collection role, second level cache region or query).
 * 
 * @author Gavin King
 */
public class CategorizedStatistics implements Serializable {
	
	private final String categoryName;

	CategorizedStatistics(String categoryName) {
		this.categoryName = categoryName;
	}
	
	public String getCategoryName() {
		return categoryName;
	}
}
