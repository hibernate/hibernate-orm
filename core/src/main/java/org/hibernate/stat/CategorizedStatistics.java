//$Id: CategorizedStatistics.java 7093 2005-06-09 06:33:06Z oneovthafew $
package org.hibernate.stat;

import java.io.Serializable;

/**
 * Statistics for a particular "category" (a named entity,
 * collection role, second level cache region or query).
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
