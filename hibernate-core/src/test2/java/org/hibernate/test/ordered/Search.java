/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Search.java 7772 2005-08-05 23:03:46Z oneovthafew $
package org.hibernate.test.ordered;
import java.util.HashSet;
import java.util.Set;

public class Search {
	private String searchString;
	private Set searchResults = new HashSet();
	
	Search() {}
	
	public Search(String string) {
		searchString = string;
	}
	
	public Set getSearchResults() {
		return searchResults;
	}
	public void setSearchResults(Set searchResults) {
		this.searchResults = searchResults;
	}
	public String getSearchString() {
		return searchString;
	}
	public void setSearchString(String searchString) {
		this.searchString = searchString;
	}
}
